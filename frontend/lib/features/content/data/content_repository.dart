import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/dio_client.dart';
import 'content_models.dart';

class ApiException implements Exception {
  final String message;
  ApiException(this.message);
  @override
  String toString() => message;
}

String _extractMessage(DioException e) {
  final data = e.response?.data;
  if (data is Map && data['message'] != null) return data['message'].toString();
  return e.message ?? 'Something went wrong. Please try again.';
}

class ContentRepository {
  final Dio _dio;
  ContentRepository(this._dio);

  // ---- Categories ----
  Future<List<CategoryItem>> listCategories() async {
    try {
      final res = await _dio.get('/categories');
      return (res.data as List).map((e) => CategoryItem.fromJson(e)).toList();
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  // ---- Files ----
  Future<PageResult<FileItem>> searchFiles({
    String? keyword,
    int? categoryId,
    String? tag,
    String? status,
    String? uploaderId,
    int page = 0,
    int size = 20,
  }) async {
    try {
      final res = await _dio.get('/files', queryParameters: {
        if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
        if (categoryId != null) 'categoryId': categoryId,
        if (tag != null && tag.isNotEmpty) 'tag': tag,
        if (status != null && status.isNotEmpty) 'status': status,
        if (uploaderId != null) 'uploaderId': uploaderId,
        'page': page,
        'size': size,
      });
      return PageResult.fromJson(res.data, (e) => FileItem.fromJson(e));
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<FileItem> getFile(String fileId) async {
    try {
      final res = await _dio.get('/files/$fileId');
      return FileItem.fromJson(res.data);
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<FileItem> uploadFile({
    required String title,
    String? description,
    int? categoryId,
    String? tags,
    required Uint8List bytes,
    required String filename,
  }) async {
    try {
      final formData = FormData.fromMap({
        'title': title,
        if (description != null) 'description': description,
        if (categoryId != null) 'categoryId': categoryId,
        if (tags != null) 'tags': tags,
        'file': MultipartFile.fromBytes(bytes, filename: filename),
      });
      final res = await _dio.post('/files', data: formData);
      return FileItem.fromJson(res.data);
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<void> deleteFile(String fileId) async {
    try {
      await _dio.delete('/files/$fileId');
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  String downloadUrl(String fileId) => '$apiBaseUrl/files/$fileId/download';

  Future<Uint8List> downloadFileBytes(String fileId) async {
    try {
      final res = await _dio.get<List<int>>('/files/$fileId/download',
          options: Options(responseType: ResponseType.bytes));
      return Uint8List.fromList(res.data ?? []);
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  // ---- Comments ----
  Future<PageResult<CommentItem>> listComments(String fileId, {int page = 0, int size = 20}) async {
    try {
      final res = await _dio.get('/files/$fileId/comments', queryParameters: {'page': page, 'size': size});
      return PageResult.fromJson(res.data, (e) => CommentItem.fromJson(e));
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<CommentItem> addComment(String fileId, String content, {String? parentId}) async {
    try {
      final res = await _dio.post('/files/$fileId/comments',
          data: {'content': content, if (parentId != null) 'parentId': parentId});
      return CommentItem.fromJson(res.data);
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<void> deleteComment(String fileId, String commentId) async {
    try {
      await _dio.delete('/files/$fileId/comments/$commentId');
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  // ---- Ratings ----
  Future<RatingSummary> getRatingSummary(String fileId) async {
    try {
      final res = await _dio.get('/files/$fileId/ratings');
      return RatingSummary.fromJson(res.data);
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<RatingSummary> rate(String fileId, int score) async {
    try {
      final res = await _dio.put('/files/$fileId/ratings', data: {'score': score});
      return RatingSummary.fromJson(res.data);
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  // ---- Favorites ----
  Future<PageResult<FileItem>> listFavorites({int page = 0, int size = 20}) async {
    try {
      final res = await _dio.get('/favorites', queryParameters: {'page': page, 'size': size});
      return PageResult.fromJson(res.data, (e) => FileItem.fromJson(e));
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<void> addFavorite(String fileId) async {
    try {
      await _dio.put('/favorites/$fileId');
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<void> removeFavorite(String fileId) async {
    try {
      await _dio.delete('/favorites/$fileId');
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  // ---- Torrents ----
  Future<TorrentInfo?> getTorrent(String fileId) async {
    try {
      final res = await _dio.get('/files/$fileId/torrent');
      return TorrentInfo.fromJson(res.data);
    } on DioException catch (e) {
      if (e.response?.statusCode == 404) return null;
      throw ApiException(_extractMessage(e));
    }
  }

  Future<TorrentInfo> generateTorrent(String fileId) async {
    try {
      final res = await _dio.post('/files/$fileId/torrent');
      return TorrentInfo.fromJson(res.data);
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  // ---- Notifications ----
  Future<PageResult<NotificationItem>> listNotifications({int page = 0, int size = 20}) async {
    try {
      final res = await _dio.get('/notifications', queryParameters: {'page': page, 'size': size});
      return PageResult.fromJson(res.data, (e) => NotificationItem.fromJson(e));
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<int> unreadNotificationCount() async {
    try {
      final res = await _dio.get('/notifications/unread-count');
      return res.data['unread'] as int? ?? 0;
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }

  Future<void> markNotificationRead(String id) async {
    try {
      await _dio.post('/notifications/$id/read');
    } on DioException catch (e) {
      throw ApiException(_extractMessage(e));
    }
  }
}

final contentRepositoryProvider = Provider<ContentRepository>((ref) {
  return ContentRepository(ref.watch(dioProvider));
});
