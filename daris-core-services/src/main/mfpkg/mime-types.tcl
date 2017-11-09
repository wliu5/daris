if { [xvalue exists [type.exists :type "image/x-nifti"]] == "false" } {
	type.create :type "image/x-nifti" :extension nii :compressable yes :description "NIFTI Image."
}

if { [xvalue exists [type.exists :type "image/x-nifti-gz"]] == "false" } {
	type.create :type "image/x-nifti-gz" :extension nii.gz :compressable no :description "Gzipped NIFTI Image."
}