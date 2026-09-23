import 'package:flutter/material.dart';

import '../../data/content_models.dart';

class FileCard extends StatelessWidget {
  final FileItem file;
  final VoidCallback onTap;
  final VoidCallback? onToggleFavorite;

  const FileCard({super.key, required this.file, required this.onTap, this.onToggleFavorite});

  Color _statusColor(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    switch (file.status) {
      case 'APPROVED':
        return Colors.green;
      case 'REJECTED':
        return scheme.error;
      default:
        return Colors.orange;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.insert_drive_file_outlined, color: theme.colorScheme.primary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(file.title,
                        maxLines: 1, overflow: TextOverflow.ellipsis, style: theme.textTheme.titleMedium),
                  ),
                  if (onToggleFavorite != null)
                    IconButton(
                      visualDensity: VisualDensity.compact,
                      icon: Icon(file.favorited ? Icons.favorite : Icons.favorite_border,
                          color: file.favorited ? Colors.redAccent : null, size: 20),
                      onPressed: onToggleFavorite,
                    ),
                ],
              ),
              const SizedBox(height: 8),
              if (file.description != null && file.description!.isNotEmpty)
                Text(file.description!,
                    maxLines: 2, overflow: TextOverflow.ellipsis, style: theme.textTheme.bodySmall),
              const SizedBox(height: 12),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: [
                  Chip(
                    label: Text(file.status, style: const TextStyle(fontSize: 11)),
                    backgroundColor: _statusColor(context).withValues(alpha: 0.15),
                    labelStyle: TextStyle(color: _statusColor(context)),
                    padding: EdgeInsets.zero,
                    visualDensity: VisualDensity.compact,
                  ),
                  if (file.categoryName != null)
                    Chip(
                      label: Text(file.categoryName!, style: const TextStyle(fontSize: 11)),
                      visualDensity: VisualDensity.compact,
                      padding: EdgeInsets.zero,
                    ),
                  ...file.tags.take(3).map((t) => Chip(
                        label: Text('#$t', style: const TextStyle(fontSize: 11)),
                        visualDensity: VisualDensity.compact,
                        padding: EdgeInsets.zero,
                      )),
                ],
              ),
              const Spacer(),
              const Divider(height: 20),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(file.readableSize, style: theme.textTheme.bodySmall),
                  Row(
                    children: [
                      Icon(Icons.star, size: 14, color: Colors.amber.shade600),
                      const SizedBox(width: 2),
                      Text((file.averageRating ?? 0).toStringAsFixed(1), style: theme.textTheme.bodySmall),
                      const SizedBox(width: 10),
                      Icon(Icons.download_outlined, size: 14, color: theme.colorScheme.outline),
                      const SizedBox(width: 2),
                      Text('${file.downloadCount}', style: theme.textTheme.bodySmall),
                    ],
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
