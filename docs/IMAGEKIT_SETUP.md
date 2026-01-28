# ImageKit File Storage Setup Guide

GigWave uses **ImageKit** for cloud-based file storage. ImageKit provides a CDN-backed storage solution with automatic image optimization, perfect for production deployments.

## Why ImageKit?

- ✅ **Free Tier Available**: 20GB storage + 20GB bandwidth per month
- ✅ **CDN Delivery**: Fast global content delivery
- ✅ **Automatic Image Optimization**: Reduces bandwidth and improves performance
- ✅ **No Server Storage**: Files stored in cloud, not on application server
- ✅ **Perfect for Railway**: No persistent volumes needed

## Setup Instructions

### 1. Create ImageKit Account

1. Go to [https://imagekit.io](https://imagekit.io)
2. Sign up for a free account
3. Create a new media library

### 2. Get Your Credentials

After creating your account, you'll get:
- **Public Key**: `public_xxxxxxxxxxxxxxxx` (from ImageKit dashboard)
- **Private Key**: `private_xxxxxxxxxxxxxxxx` (from ImageKit dashboard)
- **URL Endpoint**: `https://ik.imagekit.io/your-imagekit-id`

### 3. Configure Environment Variables

#### For Local Development

Add to your `.env` file (never commit `.env`):

```bash
FILE_STORAGE_TYPE=imagekit
IMAGEKIT_PUBLIC_KEY=public_xxxxxxxxxxxxxxxx
IMAGEKIT_PRIVATE_KEY=private_xxxxxxxxxxxxxxxx
IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/your-imagekit-id
```

Or set in `application.yml` via placeholders only (values from env):

```yaml
file:
  storage:
    type: ${FILE_STORAGE_TYPE:local}
imagekit:
  public-key: ${IMAGEKIT_PUBLIC_KEY:}
  private-key: ${IMAGEKIT_PRIVATE_KEY:}
  url-endpoint: ${IMAGEKIT_URL_ENDPOINT:}
```

#### For Railway/Production

Set these environment variables in Railway (or in `.env` for Docker):

```
FILE_STORAGE_TYPE=imagekit
IMAGEKIT_PUBLIC_KEY=public_xxxxxxxxxxxxxxxx
IMAGEKIT_PRIVATE_KEY=private_xxxxxxxxxxxxxxxx
IMAGEKIT_URL_ENDPOINT=https://ik.imagekit.io/your-imagekit-id
```

### 4. File Storage Types

The application supports two storage types:

#### ImageKit (Default - Recommended for Production)
- Files stored on ImageKit CDN
- Returns full ImageKit URLs
- No local file system required
- Perfect for cloud deployments

#### Local Storage (Development Only)
- Files stored in `uploads/` directory
- Returns relative URLs like `/uploads/chat-media/file.jpg`
- Requires persistent volume (not suitable for Railway)
- Set `FILE_STORAGE_TYPE=local` to use

### 5. File Organization

Files are automatically organized in ImageKit folders:
- `chat-media/` - Chat attachments
- `dispute-evidence/` - Dispute evidence files
- `kyc-documents/` - KYC verification documents
- `report-evidence/` - Account report evidence
- `performance-videos/` - Musician performance videos

### 6. Testing

Run the ImageKit storage tests:

```bash
cd backend
mvn test -Dtest=ImageKitFileStorageServiceTest
```

## Free Tier Limits

ImageKit free tier includes:
- **20GB Storage**
- **20GB Bandwidth** per month
- **20GB Transformations** per month

For most applications, this is sufficient. Upgrade if you exceed these limits.

## Troubleshooting

### Files Not Uploading

1. Check ImageKit credentials are correct
2. Verify `FILE_STORAGE_TYPE=imagekit` is set
3. Check file size limits (default: 10MB)
4. Review application logs for ImageKit errors

### File Deletion Issues

ImageKit file deletion requires the file ID. The service automatically extracts the file ID from the URL. If deletion fails, check:
- File URL is a valid ImageKit URL
- File exists in your ImageKit media library
- Private key has delete permissions

## Migration from Local Storage

If you were using local storage and want to migrate:

1. Set `FILE_STORAGE_TYPE=imagekit`
2. Existing local file URLs will still work (they're served by the resource handler)
3. New uploads will go to ImageKit
4. Old files can remain on local storage or be manually migrated

## Security Notes

- **Never commit private keys to git**
- Use environment variables for all credentials
- Private key should only be accessible to backend services
- Public key can be used in frontend for direct uploads (optional)

## Additional Resources

- [ImageKit Documentation](https://docs.imagekit.io/)
- [ImageKit Java SDK](https://github.com/imagekit-developer/imagekit-java)
- [ImageKit Free Tier](https://imagekit.io/pricing)
