# Page snapshot

```yaml
- generic [ref=e3]:
  - link "Skip to main content" [ref=e4] [cursor=pointer]:
    - /url: "#main-content"
  - generic [ref=e6]:
    - img "PulseLink Pro" [ref=e7]
    - heading "PulseLink Web" [level=1] [ref=e8]
    - paragraph [ref=e9]: Login to access your messages
    - generic [ref=e10]:
      - generic [ref=e11]:
        - text: Email
        - textbox "Email" [ref=e12]:
          - /placeholder: you@example.com
      - generic [ref=e13]:
        - generic [ref=e14]: Password
        - generic [ref=e15]:
          - textbox "Password" [ref=e16]:
            - /placeholder: password
          - button "Show password" [ref=e17] [cursor=pointer]:
            - img [ref=e18]
      - generic [ref=e21]:
        - button "Sign in" [ref=e22] [cursor=pointer]
        - button "Create account" [ref=e23] [cursor=pointer]
      - button "Forgot password?" [ref=e24] [cursor=pointer]
      - generic [ref=e25]: or
      - button "Sign in with Google" [ref=e26] [cursor=pointer]
```