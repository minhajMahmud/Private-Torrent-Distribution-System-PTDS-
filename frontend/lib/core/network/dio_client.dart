import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Base URL for the PTDS backend. Override at build time with
/// `--dart-define=API_BASE_URL=https://api.example.com`.
const String apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://localhost:8080/api',
);

const _secureStorage = FlutterSecureStorage();
const String _accessTokenKey = 'ptds_access_token';
const String _refreshTokenKey = 'ptds_refresh_token';

class TokenStorage {
  const TokenStorage();

  Future<void> saveTokens({required String access, required String refresh}) async {
    await _secureStorage.write(key: _accessTokenKey, value: access);
    await _secureStorage.write(key: _refreshTokenKey, value: refresh);
  }

  Future<String?> get accessToken => _secureStorage.read(key: _accessTokenKey);
  Future<String?> get refreshToken => _secureStorage.read(key: _refreshTokenKey);

  Future<void> clear() async {
    await _secureStorage.delete(key: _accessTokenKey);
    await _secureStorage.delete(key: _refreshTokenKey);
  }
}

/// Builds a configured [Dio] instance: base URL, JSON headers, JWT bearer
/// injection, and a single-retry-on-401 refresh flow.
Dio buildDioClient(TokenStorage tokenStorage) {
  final dio = Dio(BaseOptions(
    baseUrl: apiBaseUrl,
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(seconds: 15),
    headers: {'Content-Type': 'application/json'},
  ));

  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      final token = await tokenStorage.accessToken;
      if (token != null && !options.path.contains('/auth/')) {
        options.headers['Authorization'] = 'Bearer $token';
      }
      handler.next(options);
    },
    onError: (error, handler) async {
      if (error.response?.statusCode == 401) {
        final refresh = await tokenStorage.refreshToken;
        if (refresh != null) {
          try {
            final refreshDio = Dio(BaseOptions(baseUrl: apiBaseUrl));
            final response = await refreshDio.post('/auth/refresh',
                data: {'refreshToken': refresh});
            final newAccess = response.data['accessToken'] as String;
            await tokenStorage.saveTokens(access: newAccess, refresh: refresh);

            final retryRequest = error.requestOptions;
            retryRequest.headers['Authorization'] = 'Bearer $newAccess';
            final retryResponse = await dio.fetch(retryRequest);
            return handler.resolve(retryResponse);
          } catch (_) {
            await tokenStorage.clear();
          }
        }
      }
      handler.next(error);
    },
  ));

  return dio;
}

final tokenStorageProvider = Provider<TokenStorage>((ref) => const TokenStorage());

final dioProvider = Provider<Dio>((ref) {
  return buildDioClient(ref.watch(tokenStorageProvider));
});
