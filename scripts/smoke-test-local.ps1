param(
  [string]$BaseUrl = "http://localhost:8080",
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

  $uri = "$BaseUrl$Path"
  $params = @{
    Uri = $uri
    Method = $Method
    TimeoutSec = 15
  }

  if ($Headers) {
    $params.Headers = $Headers
  }

  if ($Body) {
    $params.ContentType = "application/json"
    $params.Body = Convert-ToJsonBody $Body
  }

  return Invoke-RestMethod @params
}

function Invoke-Status {
  param(
    [string]$Method,
    [string]$Path,
    [hashtable]$Headers
  )

  try {
    $params = @{
      Uri = "$BaseUrl$Path"
      Method = $Method
      UseBasicParsing = $true
      TimeoutSec = 15
    }
    if ($Headers) {
      $params.Headers = $Headers
    }
    $response = Invoke-WebRequest @params
    return [int]$response.StatusCode
  } catch {
    if ($_.Exception.Response) {
      return [int]$_.Exception.Response.StatusCode
    }
    throw
  }
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
  $health = Invoke-Api -Method "GET" -Path "/api/v1/health"
  Assert-True ($health.success -eq $true) "health response was not successful"
  Write-Ok "health"

  $categories = (Invoke-Api -Method "GET" -Path "/api/v1/categories").data
  Assert-True ($categories.Count -gt 0) "categories list was empty"
  $category = $categories[0]
  Write-Ok "public categories count=$($categories.Count)"

  $tags = (Invoke-Api -Method "GET" -Path "/api/v1/tags").data
  Assert-True ($tags.Count -gt 0) "tags list was empty"
  $tag = $tags[0]
  Write-Ok "public tags count=$($tags.Count)"

  $brands = (Invoke-Api -Method "GET" -Path "/api/v1/brands").data
  Write-Ok "public brands count=$($brands.Count)"

  $blockedStatus = Invoke-Status -Method "GET" -Path "/api/v1/admin/dashboard"
  Assert-True ($blockedStatus -eq 401) "admin dashboard without token returned $blockedStatus, expected 401"
  Write-Ok "admin dashboard blocked without token"

  $login = Invoke-Api -Method "POST" -Path "/api/v1/admin/auth/login" -Body @{
    email = $AdminEmail
    password = $AdminPassword
  }
  $token = $login.data.accessToken
  Assert-True (-not [string]::IsNullOrWhiteSpace($token)) "admin login did not return accessToken"
  Write-Ok "admin login"

  $authHeaders = @{ Authorization = "Bearer $token" }

  $dashboardStatus = Invoke-Status -Method "GET" -Path "/api/v1/admin/dashboard" -Headers $authHeaders
  Assert-True ($dashboardStatus -eq 200) "admin dashboard with token returned $dashboardStatus, expected 200"
  Write-Ok "admin dashboard with token"

  $timestamp = Get-Date -Format "yyyyMMddHHmmss"
  $today = Get-Date -Format "yyyy-MM-dd"
  $brandSlug = "smoke-brand-$timestamp"
  $brandName = "Smoke Test Brand $timestamp"

  $brand = (Invoke-Api -Method "POST" -Path "/api/v1/admin/brands" -Headers $authHeaders -Body @{
    categoryId = $category.id
    name = $brandName
    slug = $brandSlug
    description = "Smoke test brand"
    officialUrl = "https://example.com"
    isActive = $true
  }).data
  Assert-True ($brand.id -gt 0) "brand was not created"
  Write-Ok "created brand id=$($brand.id) slug=$brandSlug"

  $benefitTitle = "Smoke Test Birthday Benefit $timestamp"
  $benefit = (Invoke-Api -Method "POST" -Path "/api/v1/admin/benefits" -Headers $authHeaders -Body @{
    brandId = $brand.id
    title = $benefitTitle
    summary = "Smoke test birthday benefit summary"
    detail = "Smoke test detail"
    benefitType = "COUPON"
    occasionType = "BIRTHDAY"
    birthdayTimingType = "BIRTHDAY_MONTH"
    conditionSummary = "Smoke test condition"
    requiredApp = $false
    requiredMembership = $false
    requiredPurchase = $false
    usagePeriodDescription = "Smoke test period"
    verificationStatus = "DRAFT"
    isActive = $true
  }).data
  Assert-True ($benefit.id -gt 0) "benefit was not created"
  Write-Ok "created benefit id=$($benefit.id)"

  $source = (Invoke-Api -Method "POST" -Path "/api/v1/admin/benefits/$($benefit.id)/sources" -Headers $authHeaders -Body @{
    sourceType = "OFFICIAL_HOME"
    sourceUrl = "https://example.com"
    sourceTitle = "Smoke test official source"
    sourceCheckedAt = $today
    memo = "Smoke test source"
  }).data
  Assert-True ($source.id -gt 0) "source was not created"
  Write-Ok "created source id=$($source.id)"

  $taggedBenefit = (Invoke-Api -Method "POST" -Path "/api/v1/admin/benefits/$($benefit.id)/tags" -Headers $authHeaders -Body @{
    tagId = $tag.id
  }).data
  $linkedTags = @($taggedBenefit.tags | Where-Object { $_.slug -eq $tag.slug })
  Assert-True ($linkedTags.Count -ge 1) "tag was not linked"
  Write-Ok "added tag id=$($tag.id)"

  $publishedBenefit = (Invoke-Api -Method "PATCH" -Path "/api/v1/admin/benefits/$($benefit.id)/status" -Headers $authHeaders -Body @{
    verificationStatus = "PUBLISHED"
    lastVerifiedAt = $today
    memo = "Smoke test publish"
  }).data
  Assert-True ($publishedBenefit.verificationStatus -eq "PUBLISHED") "benefit was not published"
  Write-Ok "published benefit"

  $brandDetail = (Invoke-Api -Method "GET" -Path "/api/v1/brands/$brandSlug").data
  $publicBenefit = $brandDetail.benefits | Where-Object { $_.id -eq $benefit.id }
  Assert-True (($publicBenefit | Measure-Object).Count -eq 1) "public brand detail did not include created benefit"
  Write-Ok "public brand detail includes benefit"

  $report = (Invoke-Api -Method "POST" -Path "/api/v1/reports" -Body @{
    brandId = $brand.id
    benefitId = $benefit.id
    reportType = "WRONG_INFO"
    content = "Smoke test report says the official condition changed."
    referenceUrl = "https://example.com"
  }).data
  Assert-True ($report.id -gt 0) "report was not created"
  Write-Ok "report created id=$($report.id)"

  $needsCheckBenefit = (Invoke-Api -Method "GET" -Path "/api/v1/admin/benefits/$($benefit.id)" -Headers $authHeaders).data
  Assert-True ($needsCheckBenefit.verificationStatus -eq "NEEDS_CHECK") "benefit did not move to NEEDS_CHECK"
  Write-Ok "benefit moved to NEEDS_CHECK"

  $logs = (Invoke-Api -Method "GET" -Path "/api/v1/admin/benefits/$($benefit.id)/verification-logs" -Headers $authHeaders).data
  $logItems = @($logs)
  $publishedLog = @($logItems | Where-Object { $_.afterStatus -eq "PUBLISHED" })
  $needsCheckLog = @($logItems | Where-Object { $_.afterStatus -eq "NEEDS_CHECK" })
  Assert-True (($logItems.Count -ge 2) -and ($publishedLog.Count -ge 1) -and ($needsCheckLog.Count -ge 1)) "expected PUBLISHED and NEEDS_CHECK verification logs"
  Write-Ok "verification logs count=$($logItems.Count)"

  $reports = (Invoke-Api -Method "GET" -Path "/api/v1/admin/reports" -Headers $authHeaders).data
  $createdReport = $reports | Where-Object { $_.id -eq $report.id }
  Assert-True (($createdReport | Measure-Object).Count -eq 1) "admin reports did not include created report"
  Write-Ok "admin reports include report"

  $resolvedReport = (Invoke-Api -Method "PATCH" -Path "/api/v1/admin/reports/$($report.id)/status" -Headers $authHeaders -Body @{
    status = "RESOLVED"
    adminMemo = "Smoke test resolved"
  }).data
  Assert-True (($resolvedReport.status -eq "RESOLVED") -and (-not [string]::IsNullOrWhiteSpace($resolvedReport.resolvedAt))) "report was not resolved"
  Write-Ok "report resolved"

  Write-Host "SMOKE_TEST_OK" -ForegroundColor Green
  exit 0
} catch {
  Fail $_.Exception.Message
}
