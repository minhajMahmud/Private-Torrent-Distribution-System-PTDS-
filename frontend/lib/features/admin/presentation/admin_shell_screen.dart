import 'package:flutter/material.dart';

import 'admin_categories_screen.dart';
import 'admin_dashboard_screen.dart';
import 'admin_moderation_screen.dart';
import 'admin_users_screen.dart';

class AdminShellScreen extends StatelessWidget {
  const AdminShellScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 4,
      child: Column(
        children: [
          const TabBar(
            isScrollable: true,
            tabs: [
              Tab(text: 'Dashboard', icon: Icon(Icons.dashboard_outlined, size: 18)),
              Tab(text: 'Moderation', icon: Icon(Icons.pending_actions_outlined, size: 18)),
              Tab(text: 'Users', icon: Icon(Icons.people_outline, size: 18)),
              Tab(text: 'Categories', icon: Icon(Icons.category_outlined, size: 18)),
            ],
          ),
          const Expanded(
            child: TabBarView(
              children: [
                AdminDashboardScreen(),
                AdminModerationScreen(),
                AdminUsersScreen(),
                AdminCategoriesScreen(),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
