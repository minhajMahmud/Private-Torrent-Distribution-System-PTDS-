import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/admin_repository.dart';

class AdminDashboardScreen extends ConsumerStatefulWidget {
  const AdminDashboardScreen({super.key});

  @override
  ConsumerState<AdminDashboardScreen> createState() => _AdminDashboardScreenState();
}

class _AdminDashboardScreenState extends ConsumerState<AdminDashboardScreen> {
  AdminDashboardStats? _stats;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final stats = await ref.read(adminRepositoryProvider).dashboard();
      if (mounted) setState(() => _stats = stats);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) return Center(child: Text(_error!));
    final stats = _stats!;
    final theme = Theme.of(context);

    final cards = [
      (label: 'Total Users', value: stats.totalUsers, icon: Icons.people_outline),
      (label: 'Total Files', value: stats.totalFiles, icon: Icons.folder_outlined),
      (label: 'Pending Moderation', value: stats.pendingModeration, icon: Icons.pending_actions_outlined),
      (label: 'Approved Files', value: stats.approvedFiles, icon: Icons.check_circle_outline),
      (label: 'Rejected Files', value: stats.rejectedFiles, icon: Icons.cancel_outlined),
      (label: 'Categories', value: stats.totalCategories, icon: Icons.category_outlined),
      (label: 'Downloads (7d)', value: stats.downloadsLast7Days, icon: Icons.trending_up),
      (label: 'Locked Users', value: stats.lockedUsers, icon: Icons.lock_outline),
    ];

    return RefreshIndicator(
      onRefresh: _load,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Platform overview', style: theme.textTheme.headlineSmall),
            const SizedBox(height: 16),
            GridView.count(
              crossAxisCount: MediaQuery.sizeOf(context).width > 900 ? 4 : 2,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              childAspectRatio: 1.6,
              children: [
                for (final c in cards)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(c.icon, color: theme.colorScheme.primary),
                          const SizedBox(height: 8),
                          Text('${c.value}', style: theme.textTheme.headlineMedium),
                          Text(c.label, style: theme.textTheme.bodySmall),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 32),
            Text('File moderation status', style: theme.textTheme.titleMedium),
            const SizedBox(height: 16),
            SizedBox(
              height: 220,
              child: BarChart(
                BarChartData(
                  barGroups: [
                    BarChartGroupData(x: 0, barRods: [
                      BarChartRodData(toY: stats.approvedFiles.toDouble(), color: Colors.green)
                    ]),
                    BarChartGroupData(x: 1, barRods: [
                      BarChartRodData(toY: stats.pendingModeration.toDouble(), color: Colors.orange)
                    ]),
                    BarChartGroupData(x: 2, barRods: [
                      BarChartRodData(toY: stats.rejectedFiles.toDouble(), color: Colors.redAccent)
                    ]),
                  ],
                  titlesData: FlTitlesData(
                    bottomTitles: AxisTitles(
                      sideTitles: SideTitles(
                        showTitles: true,
                        getTitlesWidget: (value, meta) {
                          const labels = ['Approved', 'Pending', 'Rejected'];
                          return Text(labels[value.toInt().clamp(0, 2)]);
                        },
                      ),
                    ),
                    leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: true)),
                    topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                    rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  ),
                  borderData: FlBorderData(show: false),
                  gridData: const FlGridData(show: true),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
