#ifdef ANDROID

#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <limits.h>
#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include "SDL_version.h"
#include "SDL_thread.h"
#include "SDL_main.h"

/* JNI-C wrapper stuff */

#ifdef __cplusplus
#define C_LINKAGE "C"
#else
#define C_LINKAGE
#endif

#ifndef SDL_JAVA_PACKAGE_PATH
#error You have to define SDL_JAVA_PACKAGE_PATH to your package path with dots replaced with underscores, for example "com_example_SanAngeles"
#endif
#define JAVA_EXPORT_NAME2(name,package) Java_##package##_##name
#define JAVA_EXPORT_NAME1(name,package) JAVA_EXPORT_NAME2(name,package)
#define JAVA_EXPORT_NAME(name) JAVA_EXPORT_NAME1(name,SDL_JAVA_PACKAGE_PATH)

typedef int (*sdl_main_type) (int argc, char** argv);

static int argc = 0;
static char ** argv = NULL;

static JNIEnv*  static_env = NULL;
static jobject static_thiz = NULL;

JNIEnv* SDL_ANDROID_JniEnv()
{
	return static_env;
}

jobject SDL_ANDROID_JniVideoObject()
{
	return static_thiz;
}

static void* ANDROID_LoadLibrary(const char* path)
{
	void* lib = dlopen(path, RTLD_NOW | RTLD_GLOBAL);
	if(lib == NULL){
		__android_log_print(ANDROID_LOG_ERROR, "libSDLmain", "Could not load library : \"%s\"", path);
		return NULL;
	}
	__android_log_print(ANDROID_LOG_INFO, "libSDLmain", "Library loaded : \"%s\"", path);
	return lib;
}

extern C_LINKAGE void
JAVA_EXPORT_NAME(DemoRenderer_nativeInit) ( JNIEnv*  env, jobject thiz, jstring jcurdir, jstring cmdline )
{
	int i = 0;
	char curdir[PATH_MAX] = "";
	const jbyte *jstr;
	const char * str = "sdl";

	static_env = env;
	static_thiz = thiz;

	jstr = (*env)->GetStringUTFChars(env, jcurdir, NULL);
	if (jstr != NULL && strlen(jstr) > 0){
		strcpy(curdir, jstr);
	}
	(*env)->ReleaseStringUTFChars(env, jcurdir, jstr);

	chdir(curdir);
	setenv("HOME", curdir, 1);
	__android_log_print(ANDROID_LOG_INFO, "libSDL", "Changing curdir to \"%s\"", curdir);

	jstr = (*env)->GetStringUTFChars(env, cmdline, NULL);

	if (jstr != NULL && strlen(jstr) > 0){
		str = jstr;
	}
	{
		char * str1, * str2;
		str1 = strdup(str);
		str2 = str1;
		while(str2)
		{
			argc++;
			str2 = strchr(str2, ' ');
			if(!str2)
				break;
			str2++;
		}

		argv = (char **)malloc(argc*sizeof(char *));
		str2 = str1;
		while(str2)
		{
			argv[i] = str2;
			i++;
			str2 = strchr(str2, ' ');
			if(str2)
				*str2 = 0;
			else
				break;
			str2++;
		}
	}

	__android_log_print(ANDROID_LOG_INFO, "libSDL", "Calling SDL_main(\"%s\")", str);

	(*env)->ReleaseStringUTFChars(env, cmdline, jstr);

	for( i = 0; i < argc; i++ ){
		__android_log_print(ANDROID_LOG_INFO, "libSDL", "param %d = \"%s\"", i, argv[i]);
	}

	char libappPath[256];
	sprintf(libappPath, "/data/data/" SDL_ANDROID_PACKAGE_NAME "/lib/libapp_%s.so", argv[0]);
	
	void* libapp = ANDROID_LoadLibrary(libappPath);
	if(libapp){
		sdl_main_type sdl_main = (sdl_main_type)dlsym(libapp, "SDL_main");
		if(sdl_main){
			sdl_main(argc, argv);
		}
	}
};

#endif
