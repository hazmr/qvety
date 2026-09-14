## 1. Deploy
- [ ] 1.1 `deploy/docker-compose.prod.yml`, `Caddyfile`, `release.sh`, `restore.sh`, `README.md`
- [ ] 1.2 Fresh VPS to HTTPS login following only the README; time it (< 1 h)
- [ ] 1.3 Header scan: part 03 headers present, no `unsafe-inline`
- [ ] 1.4 Log rotation configured; JSON logs; no personal data in logs (test)

## 2. Backup and monitoring
- [ ] 2.1 Off-site `mc mirror` to a second provider
- [ ] 2.2 Restore drill from the off-site copy into a scratch database; log it
- [ ] 2.3 Uptime check on `/actuator/health` every 5 min; disk alert at 80 %
- [ ] 2.4 Stop the app once; confirm the phone alert

## 3. Business documents
- [ ] 3.1 Offer page in Arabic
- [ ] 3.2 Contract in Arabic; reviewed once by someone who has signed contracts
- [ ] 3.3 Operations manual: onboarding checklist, training rule, support rule, 60-day criteria, recovery numbers, runbook line
- [ ] 3.4 "Qvety demo" practice with synthetic data

## 4. Pilot
- [ ] 4.1 60-day criteria written before day one
- [ ] 4.2 Onboarding visit done per checklist
- [ ] 4.3 Support log kept for 30 days in GitHub Issues with labels
- [ ] 4.4 Day 60 review written

## 5. Close
- [ ] 5.1 `docs/progress.md` Part 16 entry: offer and price, pilot clinic and start date, 60-day result
- [ ] 5.2 Commit
