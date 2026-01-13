# Backup Strategy for GigWave

## Database Backups

### Automated Daily Backups
- **Schedule**: Daily at 2:00 AM UTC
- **Retention**: 30 days
- **Location**: S3 bucket or local backup server

### Backup Script
```bash
#!/bin/bash
# backup-db.sh
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="gigwave_backup_$DATE.archive"
mongodump --uri="$MONGODB_URI" --archive=/backups/$BACKUP_FILE
gzip /backups/$BACKUP_FILE
# Upload to S3
aws s3 cp /backups/$BACKUP_FILE.gz s3://gigwave-backups/database/
```

### Manual Backup
```bash
mongodump --uri="mongodb://localhost:27017/gigwave" --archive=backup.archive
```

## File Storage Backups

### Uploaded Files
- **Chat Media**: Daily incremental backup
- **Dispute Evidence**: Daily incremental backup  
- **KYC Documents**: Daily incremental backup with encryption

### Backup Locations
1. Primary: Local filesystem
2. Secondary: S3 bucket with versioning
3. Tertiary: Offsite backup server

## Recovery Procedures

### Database Recovery
1. Stop application
2. Restore from latest backup: `mongorestore --uri="mongodb://localhost:27017/gigwave" --archive=backup.archive`
3. Verify data integrity
4. Restart application

### File Recovery
1. Identify missing/corrupted files
2. Restore from S3 backup
3. Verify file integrity
4. Update database references if needed

## Monitoring
- Backup success/failure notifications
- Backup size monitoring
- Storage capacity alerts
- Recovery time objectives (RTO): 4 hours
- Recovery point objectives (RPO): 24 hours

