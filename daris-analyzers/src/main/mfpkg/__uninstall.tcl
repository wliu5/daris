# ============================================================================
# Include the util functions
# ============================================================================
source utils.tcl

# ============================================================================
# Uninstall Plugins
# ============================================================================
set plugin_label      [string toupper PACKAGE_$package]
set plugin_namespace  mflux/plugins
set plugin_zip        daris-analyzers-plugin.zip
set plugin_jar        daris-analyzers-plugin.jar
set plugin_path       $plugin_namespace/$plugin_jar
set module_class      nig.mf.plugin.analyzer.AnalyzerPluginModule
unloadPlugin $plugin_path $module_class
