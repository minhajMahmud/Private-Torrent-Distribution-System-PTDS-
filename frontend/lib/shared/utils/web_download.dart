// This project targets Flutter Web only (see README quick start), so a
// direct dart:html usage is acceptable here rather than pulling in extra
// cross-platform download packages for platforms this app doesn't ship to.
import 'dart:html' as html;
import 'dart:typed_data';

/// Triggers a browser "Save As" for the given bytes using a Blob + anchor
/// click — the standard Flutter Web pattern for downloads that require an
/// Authorization header (a plain `<a href>` to the API can't attach one).
void triggerBrowserDownload(Uint8List bytes, String filename, {String? mimeType}) {
  final blob = html.Blob([bytes], mimeType ?? 'application/octet-stream');
  final url = html.Url.createObjectUrlFromBlob(blob);
  final anchor = html.AnchorElement(href: url)
    ..setAttribute('download', filename)
    ..click();
  html.Url.revokeObjectUrl(url);
}
