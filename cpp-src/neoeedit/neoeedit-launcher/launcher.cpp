#include <windows.h>
#include <stdlib.h>
#include <string>
#include <tchar.h>
#include <jni.h>

#include "resource.h"



static char* filename1;
static HINSTANCE _libInst=NULL;
typedef jint (JNICALL CreateJavaVM_t)(JavaVM **pvm, void **penv, void *args);

bool file_exists(const char * filename)
{
	if (FILE * file = fopen(filename, "r"))
	{
		fclose(file);
		return true;
	}
	return false;
}
void error(const TCHAR* msg){
	MessageBox(NULL, msg, TEXT("Error"), MB_OK);   
}


bool loadJVM(){
	if (_libInst!=NULL)return false;
	char* pPath = getenv("JAVA_HOME");
	std::string jvmdll ;
	if (pPath==NULL) jvmdll=".\\jre";
	else jvmdll=pPath;
	//Util::Logger::globalLog->log("java home=%s", jvmdll.c_str());
	if (file_exists((jvmdll + "\\bin\\client\\jvm.dll").c_str())) {
		jvmdll += "\\bin\\client\\ .dll";
	} else if (file_exists((jvmdll + "\\jre\\bin\\client\\jvm.dll").c_str())) {
		//prolly a JDK
		jvmdll += "\\jre\\bin\\client\\jvm.dll";
	} else {
		//Util::Logger::globalLog->log("jvm.dll not found");
		error( TEXT("Could not find Java, Did you set JAVA_HOME correctly?"));   
		return false;
	}
	//Util::Logger::globalLog->log("jvm.dll is %s", jvmdll.c_str());
	std::wstring w1 (jvmdll.begin(), jvmdll.end());
	if ( (_libInst = LoadLibrary(w1.c_str())) == NULL) {
		std::wstring m = TEXT("Can't load "); m=m+w1;
		error(m.c_str());
		return false;
	}

	//Util::Logger::globalLog->log("dll loaded");
	return true;
}

bool runJava(){

	CreateJavaVM_t* createFn = (CreateJavaVM_t *)GetProcAddress(_libInst, "JNI_CreateJavaVM");
	if (createFn == NULL) {
		error(TEXT("Can't locate JNI_CreateJavaVM"));
		return false;
	}else{
		//Util::Logger::globalLog->log("Got JNI_CreateJavaVM @ %x", createFn);		
	}

	std::string  s1="-Djava.class.path="; s1 = s1 + filename1;
	//MessageBoxA(NULL,s1.c_str(),"debug",MB_OK); 
				
	//std::string  s2="-Djava.library.path="+ChaosDir;
	JavaVMOption options[2];
	options[0].optionString = (char *) s1.c_str();	
	//options[1].optionString =(char *)s2.c_str();
	options[1].optionString ="-Xmx512M";
	JavaVMInitArgs vm_args;
	vm_args.version = JNI_VERSION_1_6;
	vm_args.options = options;
	vm_args.nOptions = 2;
	vm_args.ignoreUnrecognized = JNI_FALSE;

	JNIEnv * x_env;
	JavaVM * x_jvm;
	jclass x_cls;
	
	/* Create the Java VM */
	jint res = createFn(&x_jvm, (void**)&x_env, &vm_args);

	if (res < 0) {
		error(TEXT("Can't create Java VM"));
		return false;
	}

	//x_jvm->AttachCurrentThreadAsDaemon((void **)&x_env,NULL);


	//entry class
	x_cls =  x_env ->FindClass("neoe/ne/Main");
	if (x_cls == 0) {
		error(TEXT("not found: target class"));
		goto destroy;
	}

	jmethodID mid = x_env->GetStaticMethodID(x_cls, "main", "([Ljava/lang/String;)V");
	if (mid == 0) {
		error(TEXT("not found: main method"));
		goto destroy;
	}
	
	// set param
	LPWSTR *szArgList;
    int argCount;
    szArgList = CommandLineToArgvW(GetCommandLine(), &argCount);

	jobjectArray applicationArgs;
	jstring applicationArg0;
	applicationArgs = x_env->NewObjectArray(argCount-1, x_env->FindClass("java/lang/String"), NULL);

	for(int i = 1; i < argCount; i++){
		std::wstring param1=szArgList[i];		
		applicationArg0 = x_env->NewString((jchar *)param1.c_str(), param1.length());
		x_env->SetObjectArrayElement(applicationArgs, i-1, applicationArg0);
	}



	x_env->CallStaticVoidMethod(x_cls, mid, applicationArgs);
	//Util::Logger::globalLog->log("JVM inited %x",x_env);	
	if (x_env->ExceptionOccurred()) {
		x_env->ExceptionDescribe();
	}
	x_jvm->DestroyJavaVM();
	return true;
destroy:

	if (x_env->ExceptionOccurred()) {
		x_env->ExceptionDescribe();
	}
	x_jvm->DestroyJavaVM();
	return false;
}
TCHAR* writeToTempFileName(LPVOID data, DWORD dataSize){
	TCHAR lpTempPathBuffer[MAX_PATH];  
    TCHAR* szTempFileName = new TCHAR[MAX_PATH];
	GetTempPath(MAX_PATH,  lpTempPathBuffer); 
	GetTempFileName(lpTempPathBuffer, TEXT("neoeedit"),0,szTempFileName);
	//wcscat(szTempFileName, TEXT(".jar"));
	HANDLE hTempFile = INVALID_HANDLE_VALUE; 
	hTempFile = CreateFile((LPTSTR) szTempFileName, // file name 
                           GENERIC_WRITE,        // open for write 
                           FILE_SHARE_READ,       
                           NULL,                 // default security 
                           CREATE_ALWAYS,        // overwrite existing
                           FILE_ATTRIBUTE_TEMPORARY ,
                           NULL); 
	if (hTempFile == INVALID_HANDLE_VALUE){
		error(TEXT("temp file create fail"));
		exit(10);
	}
	DWORD dwBytesWritten = 0; 
	bool fSuccess = WriteFile(hTempFile, 
                                 data, 
                                 dataSize,
                                 &dwBytesWritten, 
                                 NULL); 
    if (!fSuccess) 
    {
        error(TEXT("WriteFile failed"));
        exit(11);
    }
	CloseHandle(hTempFile);
	//DeleteFile(szTempFileName);
	
	return szTempFileName;	
}
static std::wstring file2delete;
bool extractResource(){
	HRSRC hRes = FindResource(0, MAKEINTRESOURCE(THE_JAR), MAKEINTRESOURCE(256));
	HGLOBAL hData = LoadResource(0, hRes);
	LPVOID data = LockResource(hData);
	DWORD dataSize = SizeofResource(0, hRes);
	TCHAR* filename = writeToTempFileName(data,dataSize);	
	std::wstring w1 = filename;
	file2delete = w1;
	std::string s1 (w1.begin(),w1.end());	
	int len = s1.length();
	filename1 = (char*)malloc((len + 1) * sizeof(char));
	strcpy_s(filename1,len+1,s1.c_str());
	return true;
}
int WINAPI WinMain(HINSTANCE hInstance,
                   HINSTANCE hPrevInstance,
                   LPSTR lpCmdLine,
                   int nCmdShow){
    if (!extractResource()) return 3;
	if (!loadJVM()) return 1;
	if (!runJava()) return 2;
		
	return 0;
}