set plugin_label      [string toupper PACKAGE_$package]
set plugin_namespace  mflux/plugins/daris-transcoders
set plugin_zip        daris-transcoders-plugin.zip
set plugin_jar        daris-transcoders-plugin.jar
set plugin_path       $plugin_namespace/$plugin_jar
set module_class      daris.transcode.DarisTranscodePluginModule

# remove transcode providers:
transcode.provider.remove :from dicom/series :to nifti/series
transcode.provider.remove :from dicom/series :to analyze/series/nl
transcode.provider.remove :from dicom/series :to analyze/series/rl
transcode.provider.remove :from dicom/series :to minc/series
transcode.provider.remove :from dicom/series :to siemens/rda
transcode.provider.remove :from bruker/series :to analyze/series/nl
transcode.provider.remove :from bruker/series :to analyze/series/rl
transcode.provider.remove :from bruker/series :to min/series


# remove plugin module
if { [xvalue exists [plugin.module.exists :path ${plugin_path} :class ${module_class}]] == "true" } {
		plugin.module.remove :path ${plugin_path} :class ${module_class}
}

# destroy the plugin jar asset.
if { [xvalue exists [asset.exists :id path=${plugin_path}]] == "true" } {
   		asset.destroy :id path=${plugin_path}
}

