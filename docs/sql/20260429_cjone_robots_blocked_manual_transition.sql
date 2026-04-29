-- CJ ONE robots.txt blocked SourceWatch manual-management transition.
-- Review before applying. This does not delete rows.

update source_watches
set is_active = false,
    collection_permission_status = 'BLOCKED_BY_ROBOTS',
    robots_check_status = 'DISALLOWED',
    collection_method = 'MANUAL_VERIFIED',
    last_policy_checked_at = current_timestamp,
    policy_check_note = concat(
        coalesce(policy_check_note, ''),
        case when policy_check_note is null or policy_check_note = '' then '' else E'\n' end,
        '2026-04-29 CJ ONE robots.txt User-agent:* Disallow:/ 확인. 자동 수집 제외 및 수동 검수 전환.'
    )
where url = 'https://m.cjone.com/cjmmobile/guide/guidePrsCpnInfo.do'
   or title = 'CJ ONE 생일축하쿠폰 안내 카드';

update benefit_candidates
set needs_manual_review = true,
    status = 'NEEDS_REVIEW',
    review_memo = concat(
        coalesce(review_memo, ''),
        case when review_memo is null or review_memo = '' then '' else E'\n' end,
        '2026-04-29 출처 robots.txt 차단으로 수동 재검토 필요.'
    )
where approved_benefit_id is null
  and status <> 'REJECTED'
  and source_watch_id in (
      select id
      from source_watches
      where url = 'https://m.cjone.com/cjmmobile/guide/guidePrsCpnInfo.do'
         or title = 'CJ ONE 생일축하쿠폰 안내 카드'
  );

update page_snapshots
set is_for_review_only = true,
    expires_at = coalesce(expires_at, current_timestamp + interval '30 days')
where source_watch_id in (
    select id
    from source_watches
    where url = 'https://m.cjone.com/cjmmobile/guide/guidePrsCpnInfo.do'
       or title = 'CJ ONE 생일축하쿠폰 안내 카드'
);
