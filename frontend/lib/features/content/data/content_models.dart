/// All read models for the content domain (files, categories, comments,
/// ratings, torrents, notifications) in one file — they're small DTOs
/// mirroring the backend response shapes 1:1.
library content_models;

class PageResult<T> {
  final List<T> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool last;

  const PageResult({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
    required this.last,
  });

  factory PageResult.fromJson(Map<String, dynamic> json, T Function(dynamic) fromItem) {
    return PageResult<T>(
      content: (json['content'] as List? ?? []).map(fromItem).toList(),
      page: json['page'] as int? ?? 0,
      size: json['size'] as int? ?? 0,
      totalElements: json['totalElements'] as int? ?? 0,
      totalPages: json['totalPages'] as int? ?? 0,
      last: json['last'] as bool? ?? true,
    );
  }

  static PageResult<T> empty<T>() => PageResult<T>(
      content: const [], page: 0, size: 0, totalElements: 0, totalPages: 0, last: true);
}

class FileItem {
  final String id;
  final String title;
  final String? description;
  final int? categoryId;
  final String? categoryName;
  final Set<String> tags;
  final String originalName;
  final int sizeBytes;
  final String? mimeType;
  final String? checksumSha256;
  final String status;
  final String? rejectionReason;
  final int downloadCount;
  final String uploaderId;
  final String uploaderUsername;
  final double? averageRating;
  final int ratingCount;
  final int commentCount;
  final bool favorited;
  final bool torrentAvailable;
  final DateTime? createdAt;

  const FileItem({
    required this.id,
    required this.title,
    this.description,
    this.categoryId,
    this.categoryName,
    required this.tags,
    required this.originalName,
    required this.sizeBytes,
    this.mimeType,
    this.checksumSha256,
    required this.status,
    this.rejectionReason,
    required this.downloadCount,
    required this.uploaderId,
    required this.uploaderUsername,
    this.averageRating,
    required this.ratingCount,
    required this.commentCount,
    required this.favorited,
    required this.torrentAvailable,
    this.createdAt,
  });

  factory FileItem.fromJson(Map<String, dynamic> json) => FileItem(
        id: json['id'] as String,
        title: json['title'] as String,
        description: json['description'] as String?,
        categoryId: json['categoryId'] as int?,
        categoryName: json['categoryName'] as String?,
        tags: Set<String>.from(json['tags'] as List? ?? const []),
        originalName: json['originalName'] as String? ?? '',
        sizeBytes: json['sizeBytes'] as int? ?? 0,
        mimeType: json['mimeType'] as String?,
        checksumSha256: json['checksumSha256'] as String?,
        status: json['status'] as String? ?? 'PENDING',
        rejectionReason: json['rejectionReason'] as String?,
        downloadCount: json['downloadCount'] as int? ?? 0,
        uploaderId: json['uploaderId'] as String? ?? '',
        uploaderUsername: json['uploaderUsername'] as String? ?? '',
        averageRating: (json['averageRating'] as num?)?.toDouble(),
        ratingCount: json['ratingCount'] as int? ?? 0,
        commentCount: json['commentCount'] as int? ?? 0,
        favorited: json['favorited'] as bool? ?? false,
        torrentAvailable: json['torrentAvailable'] as bool? ?? false,
        createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'] as String) : null,
      );

  String get readableSize {
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    double size = sizeBytes.toDouble();
    int unit = 0;
    while (size >= 1024 && unit < units.length - 1) {
      size /= 1024;
      unit++;
    }
    return '${size.toStringAsFixed(size >= 10 || unit == 0 ? 0 : 1)} ${units[unit]}';
  }
}

class CategoryItem {
  final int id;
  final String name;
  final String slug;
  final String? description;
  final int? parentId;
  final int fileCount;

  const CategoryItem({
    required this.id,
    required this.name,
    required this.slug,
    this.description,
    this.parentId,
    required this.fileCount,
  });

  factory CategoryItem.fromJson(Map<String, dynamic> json) => CategoryItem(
        id: json['id'] as int,
        name: json['name'] as String,
        slug: json['slug'] as String,
        description: json['description'] as String?,
        parentId: json['parentId'] as int?,
        fileCount: json['fileCount'] as int? ?? 0,
      );
}

class CommentItem {
  final String id;
  final String userId;
  final String username;
  final String? parentId;
  final String content;
  final DateTime? createdAt;

  const CommentItem({
    required this.id,
    required this.userId,
    required this.username,
    this.parentId,
    required this.content,
    this.createdAt,
  });

  factory CommentItem.fromJson(Map<String, dynamic> json) => CommentItem(
        id: json['id'] as String,
        userId: json['userId'] as String,
        username: json['username'] as String,
        parentId: json['parentId'] as String?,
        content: json['content'] as String,
        createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'] as String) : null,
      );
}

class RatingSummary {
  final double average;
  final int count;
  final int? myScore;

  const RatingSummary({required this.average, required this.count, this.myScore});

  factory RatingSummary.fromJson(Map<String, dynamic> json) => RatingSummary(
        average: (json['average'] as num?)?.toDouble() ?? 0.0,
        count: json['count'] as int? ?? 0,
        myScore: json['myScore'] as int?,
      );
}

class TorrentInfo {
  final String id;
  final String fileId;
  final String infoHash;
  final String magnetUri;
  final int pieceLength;
  final List<String> trackerUrls;
  final int seeders;
  final int leechers;
  final int completed;
  final String healthStatus;

  const TorrentInfo({
    required this.id,
    required this.fileId,
    required this.infoHash,
    required this.magnetUri,
    required this.pieceLength,
    required this.trackerUrls,
    required this.seeders,
    required this.leechers,
    required this.completed,
    required this.healthStatus,
  });

  factory TorrentInfo.fromJson(Map<String, dynamic> json) => TorrentInfo(
        id: json['id'] as String,
        fileId: json['fileId'] as String,
        infoHash: json['infoHash'] as String,
        magnetUri: json['magnetUri'] as String,
        pieceLength: json['pieceLength'] as int? ?? 0,
        trackerUrls: List<String>.from(json['trackerUrls'] as List? ?? const []),
        seeders: json['seeders'] as int? ?? 0,
        leechers: json['leechers'] as int? ?? 0,
        completed: json['completed'] as int? ?? 0,
        healthStatus: json['healthStatus'] as String? ?? 'UNKNOWN',
      );
}

class NotificationItem {
  final String id;
  final String type;
  final String title;
  final String? message;
  final bool read;
  final DateTime? createdAt;

  const NotificationItem({
    required this.id,
    required this.type,
    required this.title,
    this.message,
    required this.read,
    this.createdAt,
  });

  factory NotificationItem.fromJson(Map<String, dynamic> json) => NotificationItem(
        id: json['id'] as String,
        type: json['type'] as String,
        title: json['title'] as String,
        message: json['message'] as String?,
        read: json['read'] as bool? ?? false,
        createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'] as String) : null,
      );
}
