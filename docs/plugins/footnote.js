// Minimal Docsify plugin that renders reference-style footnotes ([^id] and [^id]: text).
// Based on docsify-footnote 1.0.8 by Robert-Du0001 (MIT License): https://github.com/Robert-Du0001/docsify-footnote
(function () {
  function footnote(hook) {
    hook.beforeEach(function (markdown) {
      // Keep code out of the way so that [^id] inside it stays as is.
      var code = [];
      markdown = markdown.replace(/```[\s\S]*?```|`[^`\n]*`/g, function (m) {
        code.push(m);
        return "\u0000" + (code.length - 1) + "\u0000";
      });

      var notes = {};
      markdown = markdown.replace(
        /^\[\^([^\]]+)\]:[ \t]*(.+)\n?/gm,
        function (_, id, text) {
          notes[id] = text;
          return "";
        },
      );

      var order = [];
      markdown = markdown.replace(/\[\^([^\]]+)\](?!:)/g, function (m, id) {
        if (!(id in notes)) return m;
        var n = order.indexOf(id) + 1 || order.push(id);
        // A Markdown link, not <a>, so that Docsify rewrites the anchor for its router.
        return (
          '<sup id="fnref-' + n + '">[\\[' + n + "\\]](#fn-" + n + ")</sup>"
        );
      });

      if (order.length) {
        markdown +=
          "\n\n---\n\n" +
          order
            .map(function (id, i) {
              var n = i + 1;
              return (
                n +
                ". " +
                notes[id] +
                ' <span id="fn-' +
                n +
                '">[↩︎](#fnref-' +
                n +
                ")</span>"
              );
            })
            .join("\n") +
          "\n";
      }

      return markdown.replace(/\u0000(\d+)\u0000/g, function (_, i) {
        return code[i];
      });
    });
  }

  window.$docsify = window.$docsify || {};
  window.$docsify.plugins = [].concat(window.$docsify.plugins || [], footnote);
})();
