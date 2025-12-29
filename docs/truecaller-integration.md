# Truecaller Integration for PulseLink

## Overview

PulseLink now supports Truecaller phone number lookup via RapidAPI's Truecaller4 API. This provides:

- **Caller ID**: Get name and details for unknown numbers
- **Spam Detection**: Identify spam/scam calls with confidence scores
- **Contact Verification**: Validate phone numbers when adding trusted contacts
- **Location Info**: Get geographic information about phone numbers
- **Carrier Details**: Identify mobile carrier and line type

## Security Features

✅ **API keys are NEVER hardcoded** - Loaded from secure configuration
✅ **Remote key rotation** - Keys can be updated via Firebase Remote Config without app updates
✅ **Multi-source fallback** - Tries environment variables, then local.properties, then Firebase
✅ **Production-ready** - Follows enterprise security best practices

## Setup Instructions

### 1. Get Your Truecaller API Key

1. Visit [RapidAPI Truecaller4 API](https://rapidapi.com/DataCrawler/api/truecaller4)
2. Subscribe to a plan (free tier available)
3. Copy your API key from the dashboard

⚠️ **NEVER commit your API key to source control!**

### 2. Configure Locally (Development)

Create `local.properties` in your project root (this file is gitignored):

```properties
# Truecaller API Configuration
truecallerApiKey=YOUR_RAPIDAPI_KEY_HERE
truecallerApiHost=truecaller4.p.rapidapi.com
```

### 3. Configure for CI/CD (Optional)

Set environment variables in your CI/CD pipeline:

```bash
export TRUECALLER_API_KEY="your_key_here"
export TRUECALLER_API_HOST="truecaller4.p.rapidapi.com"
```

### 4. Configure Remote Config (Production)

For production apps, store the key in Firebase Remote Config to enable rotation without app updates:

1. Go to Firebase Console → Remote Config
2. Add parameter: `truecaller_api_key`
3. Set value to your production API key
4. Publish changes

## Usage

The Truecaller integration is automatically used by PulseLink's caller ID system:

```kotlin
// The CallerIdService automatically tries all providers including Truecaller
@Inject
lateinit var callerIdService: CallerIdService

// Lookup a phone number
val result = callerIdService.lookup("+16502530000")

if (result != null) {
    Log.i("CallerID", "Name: ${result.callerName}")
    Log.i("CallerID", "Carrier: ${result.carrier}")
    Log.i("CallerID", "Location: ${result.location}")
    Log.i("CallerID", "Spam Score: ${result.spamScore}")
    Log.i("CallerID", "Is Spam: ${result.isLikelySpam}")
}
```

## Provider Priority

The Truecaller provider has **priority 2**, making it higher priority than generic RapidAPI lookup but lower than Twilio:

1. **Twilio** (priority 1) - Most reliable, premium
2. **Truecaller** (priority 2) - Good for caller ID and spam detection
3. **IPQualityScore** (priority 3) - Fraud/spam focus
4. **RapidAPI Generic** (priority 4) - Fallback option
5. **NumLookup** (priority 5) - Basic validation

## API Response Format

Truecaller API returns data in this format:

```json
{
  "data": {
    "name": "John Doe",
    "phone": "+16502530000",
    "carrier": "AT&T",
    "type": "mobile",
    "location": "California, United States",
    "spamScore": 0,
    "valid": true
  }
}
```

## Testing

### Test the API Connection

```bash
# Set your API key
export TRUECALLER_API_KEY="your_key_here"

# Test with a known number
curl -X GET \
  "https://truecaller4.p.rapidapi.com/api/v1/getDetails?phone=%2B16502530000" \
  -H "x-rapidapi-key: $TRUECALLER_API_KEY" \
  -H "x-rapidapi-host: truecaller4.p.rapidapi.com"
```

### QA Testing Checklist

- [ ] API key configured in local.properties
- [ ] App builds successfully
- [ ] Phone lookup returns correct caller information
- [ ] Spam numbers are correctly identified
- [ ] API key is NOT visible in logs
- [ ] API key is NOT in BuildConfig source code
- [ ] Remote Config override works (if configured)

## Troubleshooting

### "API key missing; skipping"

The Truecaller API key is not configured. Check:
1. Is `truecallerApiKey` set in local.properties?
2. Is `TRUECALLER_API_KEY` environment variable set?
3. Is `truecaller_api_key` configured in Firebase Remote Config?

### "failed code=429"

You've exceeded your API rate limit. Solutions:
1. Upgrade your RapidAPI plan
2. Implement request caching
3. Use Firebase Remote Config to switch to a different provider

### "failed code=401"

Invalid API key. Check:
1. Key is correctly copied from RapidAPI dashboard
2. Key hasn't been revoked
3. Subscription is still active

## Rate Limits

Check your RapidAPI subscription for current limits. Common tiers:

- **Basic (Free)**: 500 requests/month
- **Pro**: 10,000 requests/month
- **Ultra**: 100,000 requests/month
- **Mega**: 1,000,000+ requests/month

## Best Practices

1. **Cache Results**: Store lookup results to avoid duplicate API calls
2. **Use Remote Config**: Enable key rotation without app updates
3. **Monitor Usage**: Track API calls to avoid surprise bills
4. **Fallback Providers**: Don't rely on a single provider
5. **Privacy**: Only lookup numbers with user consent

## Security Reminders

🔒 **NEVER**:
- Commit API keys to git
- Hardcode keys in source code
- Share keys publicly
- Log API keys

✅ **ALWAYS**:
- Use local.properties for development
- Use environment variables for CI/CD
- Use Remote Config for production
- Rotate keys periodically

## Support

For issues with the Truecaller API:
- [RapidAPI Support](https://rapidapi.com/support)
- [Truecaller4 API Documentation](https://rapidapi.com/DataCrawler/api/truecaller4)

For PulseLink integration issues:
- Check logs for detailed error messages
- Verify API key configuration
- Test with the /api/v1/test endpoint first
