# Notification System Setup Guide

## Overview

The notification system is now **production-ready** and integrated with **Termii** - a Nigerian provider that supports SMS, Email, and WhatsApp notifications.

## What's Implemented

✅ **Termii Client** - Full integration with Termii API  
✅ **SMS Notifications** - Send SMS via Termii  
✅ **Email Notifications** - Send emails via Termii  
✅ **WhatsApp Notifications** - Send WhatsApp messages via Termii  
✅ **Automatic Phone Formatting** - Formats Nigerian numbers (adds 234 country code)  
✅ **Error Handling** - Graceful fallback if provider fails  
✅ **Logging** - All notifications logged for debugging  

## Step-by-Step Setup

### Step 1: Sign Up for Termii

1. Go to **https://termii.com**
2. Create an account
3. Verify your email
4. Complete KYC (if required)
5. Get your **API Key** from the dashboard

### Step 2: Register Sender ID (for SMS)

1. In Termii dashboard, go to **Sender ID** section
2. Register a sender ID (e.g., "GigWave")
3. Wait for approval (usually instant for test, 1-2 days for production)
4. Note your approved sender ID

### Step 3: Configure Environment Variables

Add these to your production `.env` file or environment:

```bash
# Termii API Configuration
TERMII_API_KEY=your-actual-termii-api-key-here
TERMII_SENDER_ID=GigWave  # Your approved sender ID
TERMII_BASE_URL=https://api.termii.com

# Enable notifications
NOTIFICATION_SMS_ENABLED=true
NOTIFICATION_EMAIL_ENABLED=true
NOTIFICATION_WHATSAPP_ENABLED=true

# Set providers to use Termii
NOTIFICATION_PROVIDER_SMS=termii
NOTIFICATION_PROVIDER_EMAIL=termii
NOTIFICATION_PROVIDER_WHATSAPP=termii

# Termii service flags (should match notification enabled flags)
TERMII_SMS_ENABLED=true
TERMII_EMAIL_ENABLED=true
TERMII_WHATSAPP_ENABLED=true
```

### Step 4: Test Notifications

1. Start your backend server
2. Trigger a notification (e.g., create a gig, accept a booking)
3. Check logs for notification status
4. Verify SMS/Email/WhatsApp is received

## Configuration Options

### Development Mode (Logging Only)

For development, keep these settings:

```yaml
notification:
  sms:
    enabled: false
  provider:
    sms: log  # Logs only, no real SMS sent
```

### Production Mode (Real Notifications)

For production:

```yaml
notification:
  sms:
    enabled: true
  provider:
    sms: termii  # Uses Termii API
```

## Notification Types

The system automatically sends notifications for:

1. **New Bid** - When musician bids on gig → Organizer notified
2. **New Gig** - When gig is created → All musicians notified
3. **Booking Accepted** - When organizer accepts musician → Musician notified
4. **Payment Initiated** - When payment starts → Organizer notified
5. **Payment Success** - When payment succeeds → Musician notified
6. **Payment Failed** - When payment fails → Organizer notified
7. **Gig Accepted** - When musician selected → Organizer notified

## Phone Number Formatting

The system automatically formats Nigerian phone numbers:
- `08012345678` → `2348012345678`
- `+2348012345678` → `2348012345678`
- `2348012345678` → `2348012345678` (unchanged)

## Cost Estimation (Termii)

- **SMS**: ~₦2-3 per SMS in Nigeria
- **Email**: Usually free or very low cost
- **WhatsApp**: ~₦1-2 per message

**Recommendation**: Start with SMS only, add WhatsApp/Email later if needed.

## Troubleshooting

### Notifications Not Sending?

1. **Check API Key**: Ensure `TERMII_API_KEY` is set correctly
2. **Check Enabled Flags**: Ensure `NOTIFICATION_SMS_ENABLED=true`
3. **Check Provider**: Ensure `NOTIFICATION_PROVIDER_SMS=termii`
4. **Check Logs**: Look for error messages in application logs
5. **Check Termii Dashboard**: Verify API key is active and has credits

### Common Issues

**Issue**: "SMS not enabled or API key not configured"  
**Solution**: Set `TERMII_API_KEY` and `NOTIFICATION_SMS_ENABLED=true`

**Issue**: "Failed to send SMS"  
**Solution**: 
- Check Termii dashboard for API key status
- Verify sender ID is approved
- Check account balance/credits

**Issue**: Phone number format errors  
**Solution**: System auto-formats, but ensure phone starts with 0 or 234

## Alternative Providers

If you want to use different providers, you can extend the system:

### For Twilio:
1. Add Twilio SDK dependency
2. Create `TwilioClient` similar to `TermiiClient`
3. Update `NotificationService` to support `twilio` provider

### For SendGrid (Email):
1. Add SendGrid SDK dependency
2. Create `SendGridClient`
3. Update email provider logic

## Production Checklist

- [ ] Termii account created and verified
- [ ] API key obtained from Termii dashboard
- [ ] Sender ID registered and approved
- [ ] Environment variables set in production
- [ ] `NOTIFICATION_SMS_ENABLED=true` (or email/whatsapp)
- [ ] `NOTIFICATION_PROVIDER_SMS=termii`
- [ ] Test notification sent and received
- [ ] Monitor logs for any errors
- [ ] Set up Termii account alerts for low balance

## Support

- **Termii Documentation**: https://developer.termii.com
- **Termii Support**: support@termii.com
- **Termii Dashboard**: https://termii.com/dashboard

---

**Status**: ✅ **Production Ready** - Just add your Termii API key and enable notifications!
