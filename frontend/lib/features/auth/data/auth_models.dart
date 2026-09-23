class AppUser {
  final String id;
  final String username;
  final String email;
  final Set<String> roles;

  const AppUser({
    required this.id,
    required this.username,
    required this.email,
    required this.roles,
  });

  bool get isAdmin => roles.contains('ROLE_ADMIN');

  factory AppUser.fromJson(Map<String, dynamic> json) => AppUser(
        id: json['userId'] as String,
        username: json['username'] as String,
        email: json['email'] as String,
        roles: Set<String>.from(json['roles'] as List? ?? const []),
      );
}

class AuthResult {
  final AppUser user;
  final String accessToken;
  final String refreshToken;

  const AuthResult({
    required this.user,
    required this.accessToken,
    required this.refreshToken,
  });

  factory AuthResult.fromJson(Map<String, dynamic> json) => AuthResult(
        user: AppUser.fromJson(json),
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
      );
}
