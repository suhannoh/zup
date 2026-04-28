param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$FrontendUrl = "http://localhost:3000",
  [string]$AdminEmail = "admin@zup.local",
  [string]$AdminPassword = "admin1234"
)

$ErrorActionPreference = "Stop"

function Write-Ok {
  param([string]$Message)
  Write-Host "[OK] $Message" -ForegroundColor Green
}

function Fail {
  param([string]$Message)
  Write-Host "[FAIL] $Message" -ForegroundColor Red
  exit 1
}

function Convert-ToJsonBody {
  param([hashtable]$Value)
  return ($Value | ConvertTo-Json -Depth 10)
}

function Invoke-Api {
  param(
    [string]$Method,
    [string]$Path,
    [hashtable]$Body,
    [hashtable]$Headers
  )

  $params = @{
    Uri = "$BaseUrl$Path"
    Method = $Method
    TimeoutSec = 30
  }

  if ($Headers) {
    $params.Headers = $Headers
  }

  if ($Body) {
    $params.ContentType = "application/json; charset=utf-8"
    $params.Body = Convert-ToJsonBody $Body
  }

  return Invoke-RestMethod @params
}

function Assert-True {
  param(
    [bool]$Condition,
    [string]$Message
  )

  if (-not $Condition) {
    Fail $Message
  }
}

try {
  $fixtureUrl = "$FrontendUrl/collection-fixtures/birthday-benefit.html"
  $fixtureResponse = Invoke-WebRequest -Uri $fixtureUrl -UseBasicParsing -TimeoutSec 15
  Assert-True ([int]$fixtureResponse.StatusCode -eq 200) "fixture URL was not reachable"
  Assert-True ($fixtureResponse.Content.Contains("Zup collection fixture")) "fixture did not contain expected title"
  Write-Ok "fixture reachable"

  $health = Invoke-Api -Method "GET" -Path "/api/v1/health"
  Assert-True ($health.success -eq $true) "health response was not successful"
  Write-Ok "backend health"

  $login = Invoke-Api -Method "POST" -Path "/api/v1/admin/auth/login" -Body @{
    email = $AdminEmail
    password = $AdminPassword
  }
  $token = $login.data.accessToken
  Assert-True (-not [string]::IsNullOrWhiteSpace($token)) "admin login did not return accessToken"
  $authHeaders = @{ Authorization = "Bearer $token" }
  Write-Ok "admin login"

  $brands = @((Invoke-Api -Method "GET" -Path "/api/v1/admin/brands" -Headers $authHeaders).data)
  if ($brands.Count -gt 0) {
    $brand = $brands[0]
    Write-Ok "using existing brand id=$($brand.id)"
  } else {
    $categories = @((Invoke-Api -Method "GET" -Path "/api/v1/categories").data)
    Assert-True ($categories.Count -gt 0) "no category available to create smoke brand"
    $timestamp = Get-Date -Format "yyyyMMddHHmmss"
    $brand = (Invoke-Api -Method "POST" -Path "/api/v1/admin/brands" -Headers $authHeaders -Body @{
      categoryId = $categories[0].id
      name = "Zup Collection Fixture Brand $timestamp"
      slug = "collection-fixture-brand-$timestamp"
      description = "Local collection fixture smoke brand"
      officialUrl = $FrontendUrl
      isActive = $true
    }).data
    Write-Ok "created smoke brand id=$($brand.id)"
  }

  $timestamp = Get-Date -Format "yyyyMMddHHmmss"
  $sourceWatch = (Invoke-Api -Method "POST" -Path "/api/v1/admin/source-watches" -Headers $authHeaders -Body @{
    brandId = $brand.id
    sourceType = "OFFICIAL_HOME"
    title = "Local collection fixture $timestamp"
    url = $fixtureUrl
    isActive = $true
  }).data
  Assert-True ($sourceWatch.id -gt 0) "SourceWatch was not created"
  Write-Ok "created SourceWatch id=$($sourceWatch.id)"

  $collect = (Invoke-Api -Method "POST" -Path "/api/v1/admin/source-watches/$($sourceWatch.id)/collect" -Headers $authHeaders).data
  Assert-True ($collect.fetched -eq $true) "collect did not fetch"
  Assert-True ($collect.candidateCount -ge 1) "collect candidateCount was less than 1"
  Write-Ok "collect candidateCount=$($collect.candidateCount) sameAsPrevious=$($collect.sameAsPrevious)"

  $candidates = @((Invoke-Api -Method "GET" -Path "/api/v1/admin/benefit-candidates" -Headers $authHeaders).data)
  $candidate = @($candidates | Where-Object { $_.sourceWatchId -eq $sourceWatch.id } | Select-Object -First 1)[0]
  Assert-True ($null -ne $candidate) "candidate for SourceWatch was not found"
  Assert-True ($candidate.status -eq "NEEDS_REVIEW") "candidate status was not NEEDS_REVIEW"
  Write-Ok "candidate id=$($candidate.id)"

  $approve = (Invoke-Api -Method "POST" -Path "/api/v1/admin/benefit-candidates/$($candidate.id)/approve" -Headers $authHeaders -Body @{
    title = "Collection fixture approved benefit $timestamp"
    summary = "Local fixture candidate approved into a VERIFIED benefit"
    benefitType = $candidate.benefitType
    occasionType = "BIRTHDAY"
    birthdayTimingType = $candidate.birthdayTimingType
    birthdayTimingDescription = "Fixture birthday month"
    requiresApp = $candidate.requiresApp
    requiresSignup = $candidate.requiresSignup
    requiresMembership = $candidate.requiresMembership
    minimumPurchaseDescription = $null
    usageCondition = $candidate.evidenceText
    adminMemo = "Local collection smoke test approval"
  }).data
  Assert-True ($approve.benefitId -gt 0) "approve did not return benefitId"
  Assert-True ($approve.verificationStatus -eq "VERIFIED") "approved benefit was not VERIFIED"
  Write-Ok "approved candidate benefitId=$($approve.benefitId)"

  $approvedCandidate = (Invoke-Api -Method "GET" -Path "/api/v1/admin/benefit-candidates/$($candidate.id)" -Headers $authHeaders).data
  Assert-True ($approvedCandidate.approvedBenefitId -eq $approve.benefitId) "candidate approvedBenefitId did not match"
  Write-Ok "candidate approvedBenefitId saved"

  $adminBenefit = (Invoke-Api -Method "GET" -Path "/api/v1/admin/benefits/$($approve.benefitId)" -Headers $authHeaders).data
  Assert-True ($adminBenefit.id -eq $approve.benefitId) "admin benefit lookup failed"
  Assert-True ($adminBenefit.verificationStatus -eq "VERIFIED") "admin benefit was not VERIFIED"
  Write-Ok "admin benefit is VERIFIED"

  $publicBenefits = @((Invoke-Api -Method "GET" -Path "/api/v1/benefits").data)
  $publishedMatch = @($publicBenefits | Where-Object { $_.id -eq $approve.benefitId })
  Assert-True ($publishedMatch.Count -eq 0) "VERIFIED benefit appeared in public benefit API"
  Write-Ok "public API does not expose VERIFIED benefit"

  Write-Host "COLLECTION_SMOKE_TEST_OK" -ForegroundColor Green
  exit 0
} catch {
  Fail $_.Exception.Message
}
