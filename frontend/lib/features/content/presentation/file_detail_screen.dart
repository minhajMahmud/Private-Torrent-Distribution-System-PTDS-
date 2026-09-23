import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'dart:html' as html;

import '../../auth/application/auth_controller.dart';
import '../data/content_models.dart';
import '../data/content_repository.dart';
import '../../../shared/utils/web_download.dart';

class FileDetailScreen extends ConsumerStatefulWidget {
  final String fileId;
  const FileDetailScreen({super.key, required this.fileId});

  @override
  ConsumerState<FileDetailScreen> createState() => _FileDetailScreenState();
}

class _FileDetailScreenState extends ConsumerState<FileDetailScreen> {
  FileItem? _file;
  RatingSummary? _rating;
  TorrentInfo? _torrent;
  PageResult<CommentItem> _comments = PageResult.empty<CommentItem>();
  final _commentController = TextEditingController();
  bool _loading = true;
  bool _downloading = false;
  bool _generatingTorrent = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final repo = ref.read(contentRepositoryProvider);
    try {
      final results = await Future.wait([
        repo.getFile(widget.fileId),
        repo.getRatingSummary(widget.fileId),
        repo.listComments(widget.fileId),
      ]);
      _file = results[0] as FileItem;
      _rating = results[1] as RatingSummary;
      _comments = results[2] as PageResult<CommentItem>;
      if (_file!.torrentAvailable) {
        _torrent = await repo.getTorrent(widget.fileId);
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _toggleFavorite() async {
    if (_file == null) return;
    final repo = ref.read(contentRepositoryProvider);
    try {
      if (_file!.favorited) {
        await repo.removeFavorite(_file!.id);
      } else {
        await repo.addFavorite(_file!.id);
      }
      _load();
    } catch (e) {
      _showError(e);
    }
  }

  Future<void> _download() async {
    if (_file == null) return;
    setState(() => _downloading = true);
    try {
      final repo = ref.read(contentRepositoryProvider);
      final bytes = await repo.downloadFileBytes(_file!.id);
      triggerBrowserDownload(bytes, _file!.originalName, mimeType: _file!.mimeType);
      _load();
    } catch (e) {
      _showError(e);
    } finally {
      if (mounted) setState(() => _downloading = false);
    }
  }

  Future<void> _generateTorrent() async {
    setState(() => _generatingTorrent = true);
    try {
      final torrent = await ref.read(contentRepositoryProvider).generateTorrent(widget.fileId);
      setState(() => _torrent = torrent);
    } catch (e) {
      _showError(e);
    } finally {
      if (mounted) setState(() => _generatingTorrent = false);
    }
  }

  void _openMagnet() {
    if (_torrent != null) html.window.open(_torrent!.magnetUri, '_blank');
  }

  Future<void> _rate(int score) async {
    try {
      final summary = await ref.read(contentRepositoryProvider).rate(widget.fileId, score);
      setState(() => _rating = summary);
    } catch (e) {
      _showError(e);
    }
  }

  Future<void> _postComment() async {
    final text = _commentController.text.trim();
    if (text.isEmpty) return;
    try {
      await ref.read(contentRepositoryProvider).addComment(widget.fileId, text);
      _commentController.clear();
      final comments = await ref.read(contentRepositoryProvider).listComments(widget.fileId);
      setState(() => _comments = comments);
    } catch (e) {
      _showError(e);
    }
  }

  void _showError(Object e) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
  }

  Color _healthColor(String status) {
    switch (status) {
      case 'HEALTHY':
        return Colors.green;
      case 'LOW_SEEDS':
        return Colors.orange;
      case 'DEAD':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final authState = ref.watch(authControllerProvider);
    final isAdmin = authState is AuthAuthenticated && authState.user.isAdmin;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => context.pop()),
        title: Text(_file?.title ?? 'File details'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!))
              : _buildBody(context, theme, isAdmin),
    );
  }

  Widget _buildBody(BuildContext context, ThemeData theme, bool isAdmin) {
    final file = _file!;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 760),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(child: Text(file.title, style: theme.textTheme.headlineSmall)),
                  IconButton(
                    icon: Icon(file.favorited ? Icons.favorite : Icons.favorite_border,
                        color: file.favorited ? Colors.redAccent : null),
                    onPressed: _toggleFavorite,
                  ),
                ],
              ),
              Text('by ${file.uploaderUsername} · ${file.readableSize} · ${file.status}',
                  style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
              const SizedBox(height: 16),
              if (file.description != null) Text(file.description!),
              const SizedBox(height: 16),
              Wrap(spacing: 8, children: file.tags.map((t) => Chip(label: Text('#$t'))).toList()),
              const SizedBox(height: 24),

              Row(
                children: [
                  FilledButton.icon(
                    onPressed: file.status == 'APPROVED' && !_downloading ? _download : null,
                    icon: _downloading
                        ? const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.download),
                    label: const Text('Download'),
                  ),
                  const SizedBox(width: 12),
                  if (file.status == 'APPROVED')
                    OutlinedButton.icon(
                      onPressed: _torrent == null
                          ? (_generatingTorrent ? null : _generateTorrent)
                          : _openMagnet,
                      icon: const Icon(Icons.hub_outlined),
                      label: Text(_torrent == null ? 'Generate torrent' : 'Open magnet link'),
                    ),
                ],
              ),

              if (_torrent != null) _buildTorrentPanel(theme),

              const SizedBox(height: 32),
              Text('Rating', style: theme.textTheme.titleMedium),
              Row(
                children: [
                  ...List.generate(5, (i) {
                    final filled = (i + 1) <= (_rating?.myScore ?? 0);
                    return IconButton(
                      icon: Icon(filled ? Icons.star : Icons.star_border, color: Colors.amber),
                      onPressed: () => _rate(i + 1),
                    );
                  }),
                  Text('${(_rating?.average ?? 0).toStringAsFixed(1)} (${_rating?.count ?? 0} ratings)'),
                ],
              ),

              const SizedBox(height: 24),
              Text('Comments (${_comments.totalElements})', style: theme.textTheme.titleMedium),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _commentController,
                      decoration: const InputDecoration(hintText: 'Add a comment...', isDense: true),
                      onSubmitted: (_) => _postComment(),
                    ),
                  ),
                  IconButton(icon: const Icon(Icons.send), onPressed: _postComment),
                ],
              ),
              const SizedBox(height: 12),
              ..._comments.content.map((c) => ListTile(
                    contentPadding: EdgeInsets.zero,
                    leading: CircleAvatar(child: Text(c.username.isNotEmpty ? c.username[0].toUpperCase() : '?')),
                    title: Text(c.username),
                    subtitle: Text(c.content),
                  )),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTorrentPanel(ThemeData theme) {
    final torrent = _torrent!;
    return Card(
      margin: const EdgeInsets.only(top: 16),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.circle, size: 10, color: _healthColor(torrent.healthStatus)),
                const SizedBox(width: 8),
                Text('Torrent health: ${torrent.healthStatus}', style: theme.textTheme.titleSmall),
              ],
            ),
            const SizedBox(height: 8),
            Text('Seeders: ${torrent.seeders}  ·  Leechers: ${torrent.leechers}  ·  Completed: ${torrent.completed}'),
            const SizedBox(height: 8),
            Text('Info hash: ${torrent.infoHash}', style: theme.textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}
