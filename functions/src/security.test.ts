import {escapeHtml} from "./security";

describe("escapeHtml", () => {
  describe("Basic HTML escaping", () => {
    it("should escape ampersand", () => {
      expect(escapeHtml("foo & bar")).toBe("foo &amp; bar");
    });

    it("should escape less than", () => {
      expect(escapeHtml("a < b")).toBe("a &lt; b");
    });

    it("should escape greater than", () => {
      expect(escapeHtml("a > b")).toBe("a &gt; b");
    });

    it("should escape double quotes", () => {
      expect(escapeHtml('say "hello"')).toBe("say &quot;hello&quot;");
    });

    it("should escape single quotes", () => {
      expect(escapeHtml("it's nice")).toBe("it&#039;s nice");
    });
  });

  describe("XSS attack prevention", () => {
    it("should escape basic script tag", () => {
      const input = '<script>alert("xss")</script>';
      const expected = "&lt;script&gt;alert(&quot;xss&quot;)&lt;/script&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should escape img tag with onerror", () => {
      const input = '<img src="x" onerror="alert(1)">';
      const expected = "&lt;img src=&quot;x&quot; onerror=&quot;alert(1)&quot;&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should escape iframe injection", () => {
      const input = '<iframe src="javascript:alert(1)"></iframe>';
      const expected = "&lt;iframe src=&quot;javascript:alert(1)&quot;&gt;&lt;/iframe&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should escape SVG with javascript", () => {
      const input = '<svg onload="alert(1)">';
      const expected = "&lt;svg onload=&quot;alert(1)&quot;&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should escape style tag", () => {
      const input = '<style>body{display:none}</style>';
      const expected = "&lt;style&gt;body{display:none}&lt;/style&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should escape anchor tag with javascript", () => {
      const input = '<a href="javascript:alert(1)">click</a>';
      const expected = "&lt;a href=&quot;javascript:alert(1)&quot;&gt;click&lt;/a&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });
  });

  describe("Multiple character escaping", () => {
    it("should escape all special characters in one string", () => {
      const input = '&<>"\'';
      const expected = "&amp;&lt;&gt;&quot;&#039;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should handle complex HTML with multiple entities", () => {
      const input = '<div class="container" id=\'main\'>Content & more</div>';
      const expected = "&lt;div class=&quot;container&quot; id=&#039;main&#039;&gt;Content &amp; more&lt;/div&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should escape nested tags", () => {
      const input = '<div><span>test</span></div>';
      const expected = "&lt;div&gt;&lt;span&gt;test&lt;/span&gt;&lt;/div&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });
  });

  describe("Edge cases and type safety", () => {
    it("should handle empty string", () => {
      expect(escapeHtml("")).toBe("");
    });

    it("should handle null input", () => {
      expect(escapeHtml(null)).toBe("");
    });

    it("should handle undefined input", () => {
      expect(escapeHtml(undefined)).toBe("");
    });

    it("should handle string with no special characters", () => {
      const input = "Hello World";
      expect(escapeHtml(input)).toBe("Hello World");
    });

    it("should handle numbers by converting to string", () => {
      // @ts-expect-error Testing runtime type coercion
      expect(escapeHtml(123)).toBe("123");
    });

    it("should handle boolean by converting to string", () => {
      // @ts-expect-error Testing runtime type coercion
      expect(escapeHtml(true)).toBe("true");
    });

    it("should handle objects by converting to string", () => {
      // @ts-expect-error Testing runtime type coercion
      expect(escapeHtml({})).toBe("[object Object]");
    });
  });

  describe("Unicode and special characters", () => {
    it("should preserve Unicode characters", () => {
      const input = "Hello 世界 🌍";
      expect(escapeHtml(input)).toBe("Hello 世界 🌍");
    });

    it("should escape HTML entities while preserving Unicode", () => {
      const input = "<script>世界</script>";
      const expected = "&lt;script&gt;世界&lt;/script&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should handle emoji", () => {
      const input = "Hello 👋 <script>alert('xss')</script>";
      const expected = "Hello 👋 &lt;script&gt;alert(&#039;xss&#039;)&lt;/script&gt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should handle accented characters", () => {
      const input = "Café & Restaurant";
      expect(escapeHtml(input)).toBe("Café &amp; Restaurant");
    });
  });

  describe("Real-world email scenarios", () => {
    it("should escape user names with special characters", () => {
      const userName = "O'Brien & Associates";
      const expected = "O&#039;Brien &amp; Associates";
      expect(escapeHtml(userName)).toBe(expected);
    });

    it("should escape message content", () => {
      const message = 'User said: "Hello <friend>"';
      const expected = "User said: &quot;Hello &lt;friend&gt;&quot;";
      expect(escapeHtml(message)).toBe(expected);
    });

    it("should escape link codes safely", () => {
      const code = "ABC<>123";
      const expected = "ABC&lt;&gt;123";
      expect(escapeHtml(code)).toBe(expected);
    });
  });

  describe("Order of replacements", () => {
    it("should replace ampersand first to avoid double encoding", () => {
      // This test verifies that & is replaced first
      // so that &lt; doesn't become &amp;lt;
      const input = "&lt;";
      const expected = "&amp;lt;";
      expect(escapeHtml(input)).toBe(expected);
    });

    it("should not double-encode already encoded entities", () => {
      const input = "&amp;";
      const expected = "&amp;amp;";
      expect(escapeHtml(input)).toBe(expected);
    });
  });

  describe("Length and performance", () => {
    it("should handle long strings", () => {
      const longString = "<script>".repeat(1000);
      const result = escapeHtml(longString);
      expect(result).toContain("&lt;script&gt;");
      expect(result.length).toBeGreaterThan(longString.length);
    });

    it("should handle very long string without special chars", () => {
      const longString = "a".repeat(10000);
      expect(escapeHtml(longString)).toBe(longString);
    });
  });

  describe("Whitespace preservation", () => {
    it("should preserve spaces", () => {
      const input = "   spaces   ";
      expect(escapeHtml(input)).toBe("   spaces   ");
    });

    it("should preserve newlines", () => {
      const input = "line1\nline2\nline3";
      expect(escapeHtml(input)).toBe("line1\nline2\nline3");
    });

    it("should preserve tabs", () => {
      const input = "tab\there";
      expect(escapeHtml(input)).toBe("tab\there");
    });

    it("should preserve carriage returns", () => {
      const input = "line1\r\nline2";
      expect(escapeHtml(input)).toBe("line1\r\nline2");
    });
  });
});
