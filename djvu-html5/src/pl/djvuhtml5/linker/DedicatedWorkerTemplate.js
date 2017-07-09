// Workaround for generated code accessing window and document
// when it is not really window and document which are needed
self.$wnd = self;
self.$doc = self;

function __MODULE_FUNC__() {
 var strongName;
 try {
 // __PERMUTATIONS_BEGIN__
 // Permutation logic
 // __PERMUTATIONS_END__
 } catch (e) {
 // intentionally silent on property failure
 return;
 }
 importScripts(strongName + ".cache.js");
 gwtOnLoad(undefined, '__MODULE_NAME__', '');
}
__MODULE_FUNC__();