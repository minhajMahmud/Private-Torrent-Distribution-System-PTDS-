import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../auth/application/auth_controller.dart';
import '../data/content_models.dart';
import '../data/content_repository.dart';
import 'widgets/file_card.dart';

enum FileBrowseMode { all, mine, favorites }

class FileBrowseScreen extends ConsumerStatefulWidget {
  final FileBrowseMode mode;
  const FileBrowseScreen({super.key, this.mode = FileBrowseMode.all});

  @override
  ConsumerState<FileBrowseScreen> createState() => _FileBrowseScreenState();
}

class _FileBrowseScreenState extends ConsumerState<FileBrowseScreen> {
  final _searchController = TextEditingController();
  List<CategoryItem> _categories = [];
  int? _selectedCategoryId;
  String _keyword = '';
  int _page = 0;
  PageResult<FileItem> _result = PageResult.empty<FileItem>();
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadCategories();
    _load();
  }

  Future<void> _loadCategories() async {
    if (widget.mode != FileBrowseMode.all) return;
    try {
      final cats = await ref.read(contentRepositoryProvider).listCategories();
      if (mounted) setState(() => _categories = cats);
    } catch (_) {
      // Non-fatal — filters simply won't be available.
    }
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final repo = ref.read(contentRepositoryProvider);
      final authState = ref.read(authControllerProvider);
      final userId = authState is AuthAuthenticated ? authState.user.id : null;

      final PageResult<FileItem> result;
      switch (widget.mode) {
        case FileBrowseMode.favorites:
          result = await repo.listFavorites(page: _page, size: 12);
          break;
        case FileBrowseMode.mine:
          result = await repo.searchFiles(
              keyword: _keyword, categoryId: _selectedCategoryId, uploaderId: userId, page: _page, size: 12);
          break;
        case FileBrowseMode.all:
          result = await repo.searchFiles(
              keyword: _keyword, categoryId: _selectedCategoryId, page: _page, size: 12);
          break;
      }
      if (mounted) setState(() => _result = result);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _toggleFavorite(FileItem file) async {
    final repo = ref.read(contentRepositoryProvider);
    try {
      if (file.favorited) {
        await repo.removeFavorite(file.id);
      } else {
        await repo.addFavorite(file.id);
      }
      _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (widget.mode == FileBrowseMode.all)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _searchController,
                    decoration: const InputDecoration(
                      prefixIcon: Icon(Icons.search),
                      hintText: 'Search titles and descriptions...',
                      isDense: true,
                    ),
                    onSubmitted: (v) {
                      _keyword = v;
                      _page = 0;
                      _load();
                    },
                  ),
                ),
                const SizedBox(width: 12),
                if (_categories.isNotEmpty)
                  DropdownButton<int?>(
                    value: _selectedCategoryId,
                    hint: const Text('Category'),
                    items: [
                      const DropdownMenuItem(value: null, child: Text('All categories')),
                      ..._categories.map((c) => DropdownMenuItem(value: c.id, child: Text(c.name))),
                    ],
                    onChanged: (v) {
                      setState(() => _selectedCategoryId = v);
                      _page = 0;
                      _load();
                    },
                  ),
              ],
            ),
          ),
        Expanded(child: _buildBody(context)),
        if (_result.totalPages > 1)
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                IconButton(
                  onPressed: _page > 0 ? () { setState(() => _page--); _load(); } : null,
                  icon: const Icon(Icons.chevron_left),
                ),
                Text('Page ${_page + 1} of ${_result.totalPages}'),
                IconButton(
                  onPressed: !_result.last ? () { setState(() => _page++); _load(); } : null,
                  icon: const Icon(Icons.chevron_right),
                ),
              ],
            ),
          ),
      ],
    );
  }

  Widget _buildBody(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(_error!, textAlign: TextAlign.center),
            const SizedBox(height: 12),
            FilledButton(onPressed: _load, child: const Text('Retry')),
          ],
        ),
      );
    }
    if (_result.content.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.inbox_outlined, size: 48, color: Theme.of(context).colorScheme.outline),
            const SizedBox(height: 12),
            Text(widget.mode == FileBrowseMode.favorites
                ? 'No favorites yet'
                : widget.mode == FileBrowseMode.mine
                    ? "You haven't uploaded anything yet"
                    : 'No files found'),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _load,
      child: GridView.builder(
        padding: const EdgeInsets.all(16),
        gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
          maxCrossAxisExtent: 340,
          mainAxisExtent: 210,
          crossAxisSpacing: 16,
          mainAxisSpacing: 16,
        ),
        itemCount: _result.content.length,
        itemBuilder: (context, i) {
          final file = _result.content[i];
          return FileCard(
            file: file,
            onTap: () => context.push('/files/${file.id}'),
            onToggleFavorite: () => _toggleFavorite(file),
          );
        },
      ),
    );
  }
}
