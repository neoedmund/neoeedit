
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <windows.h>
#include <jni.h>
#include <tchar.h>


#define MAX_PATH_1000 1000
typedef jint ( JNICALL CreateJavaVM_t )( JavaVM * * vm , void * * env , void * args ) ;
void error( const TCHAR * msg ) {
	MessageBox( NULL , msg , TEXT( "Error" ) , MB_OK ) ;
}
bool prefix( const char * pre , const char * str )
{
	return strncmp( pre , str , strlen( pre ) ) == 0 ;
}
int getVer( char * fn , const char * pre ) {
//	printf( "get ver [%s] and [%s]\n" , fn , pre ) ;

	char * start = fn + strlen( pre ) ;
	char * p1 = strstr( start , "." ) ;
	if ( p1 == NULL ) return 0 ;
	char sub1 [ MAX_PATH_1000 ] ;
	strncpy_s( sub1 , sizeof( sub1 ) , start , p1 - start ) ;
	sub1 [ p1 - start ] = 0 ;
//	printf( "ver str=[%s]\n" , sub1 ) ;
	return strtol( sub1 , ( char * * ) NULL , 10 ) ;
}
char * str_clone( char * p3 , int len ) {
	char * p4 = ( char * ) malloc( len + 1 ) ;
	strncpy_s( p4 , len + 1 , p3 , len ) ;
	p4 [ len ] = 0 ;
	return p4 ;
}

char * searchPath( const char * p1 ) {
	char * drive = NULL ;
	size_t size10 = 0 ;
	_dupenv_s( & drive , & size10 , "SystemDrive" ) ;
	if ( drive == NULL ) {
		drive =( char * ) "c:" ;
	}
	char buf [ 2048 ] ;
	sprintf( buf , "%s\\%s\\*" , drive , p1 ) ;
	int ver = 0 ;
	int kind = 0 ;
	//jdk,jre
	char p2 [ MAX_PATH_1000 ] ;
	WIN32_FIND_DATA FindFileData ;
	HANDLE hFind = FindFirstFile( buf , & FindFileData ) ;
	if( hFind == INVALID_HANDLE_VALUE ) {
		printf( "cannot list [%s]\n" , buf ) ;
	} else {
		const char * preJdk = "jdk-" ;
		const char * preJre = "jre1." ;
		while( 1 ) {
		//	printf( "[%s]\n" , FindFileData . cFileName ) ;
			char * fn = FindFileData . cFileName ;
			int v1 = 0 ;
			int kind1 = 0 ;
			if ( prefix( preJdk , fn ) ) {
				v1 = getVer( fn , preJdk ) ;
			} else if ( prefix( preJre , fn ) ) {
				v1 = getVer( fn , preJre ) ;
				kind1 = 1 ;
			}
			if ( v1 > ver ) {
				ver = v1 ;
				kind = kind1 ;
				int len = strlen( fn ) ;
				strncpy_s( p2 , sizeof( p2 ) , fn , len ) ;
				p2 [ len ] = 0 ;
			}
			if ( ! FindNextFile( hFind , & FindFileData ) ) break ;
		}
		FindClose( hFind ) ;
	}
	if ( ver > 0 ) {
		printf( "find %s %d at [%s]\n" , kind == 0 ? "JDK" : "JRE" , ver , p2 ) ;
		char p3 [ MAX_PATH_1000 ] ;
		int len = sprintf( p3 , "%s\\%s\\%s\\bin\\server\\jvm.dll" , drive , p1 , p2 ) ;
		return str_clone( p3 , len ) ;
	}
	return NULL ;
}
static HINSTANCE _libInst = NULL ;
void find( ) {
	char * path = searchPath( "Program Files\\Java\\" ) ;
//	printf( "Found JDK(0)[%s]\n" , path ) ;
	//path = "C:\\Program Files\\Java\\jre1.8.0_301" ;
	printf( "Found JDK[%s]\n" , path ) ;
	char * jvmdll = path ;
	// "C:\\Program Files\\Java\\jre1.8.0_301\\bin\\server\\jvm.dll" ;

	TCHAR szPath [ MAX_PATH_1000 ] ;
	GetModuleFileName( NULL , szPath , MAX_PATH_1000 ) ;
	{
		char * buf = szPath ;
		int p1 = strlen( buf ) -1 ;
		while( buf [ p1 ] != '\\' && p1 > 0 ) p1 -- ;
		if ( p1 >= 0 ) buf [ p1 ] = 0 ;
	}

	if ( ( _libInst = LoadLibrary( jvmdll ) ) == NULL ) {
		printf( "cannot load %s\n" , jvmdll ) ;
		return ;
	}
	CreateJavaVM_t * createFunc = ( CreateJavaVM_t * ) GetProcAddress( _libInst , "JNI_CreateJavaVM" ) ;
	if ( createFunc == NULL ) {
		printf( "Can't locate JNI_CreateJavaVM\n" ) ;
		return ;
	}
	JNIEnv * env ;
	JavaVM * vm ;
	JavaVMInitArgs vm_args ;
	JavaVMOption options [ 2 ] ;
	{ char buf [ MAX_PATH_1000 ] ;
		sprintf_s( buf , MAX_PATH_1000 , "-Djava.class.path=%s\\neoeedit.jar" , szPath ) ;
		options [ 0 ] . optionString = ( char * ) buf ;
	}
	options [ 1 ] . optionString = ( char * ) "-Xrs" ;
	vm_args . version = JNI_VERSION_1_8 ;
	vm_args . nOptions = 2 ;
	vm_args . options = options ;
	vm_args . ignoreUnrecognized = 1 ;

	jint res = createFunc( & vm , ( void * * ) & env , & vm_args ) ;
	if ( res < 0 ) {
		printf( "Can't create JVM\n" ) ;
		return ;
	}
	//printf( "res=%d|%X\n" , res , env ) ;
	jclass cls = env -> FindClass( "neoe/ne/Main" ) ;
	if ( cls == 0 ) {
		printf( "main class not found\n" ) ;
		exit( 1 ) ;
	}
	jmethodID mid = env -> GetStaticMethodID( cls , "main" , "([Ljava/lang/String;)V" ) ;
	if ( mid == 0 ) {
		printf( "main() method not found\n" ) ;
		exit( 1 ) ;
	}
	int argCount ;
	LPWSTR * szArgList ;
	szArgList = CommandLineToArgvW( GetCommandLineW( ) , & argCount ) ;

	jobjectArray args = env -> NewObjectArray( argCount -1 , env -> FindClass( "java/lang/String" ) , NULL ) ;
	for( int i = 1 ;
		i < argCount ;
		i ++ ) {
		jstring arg1 = env -> NewString(( jchar * ) szArgList [ i ] , lstrlenW( szArgList [ i ] ) ) ;
		env -> SetObjectArrayElement( args , i -1 , arg1 ) ;
	}
	env -> CallStaticVoidMethod( cls , mid , args ) ;
}

int WINAPI WinMain( HINSTANCE hInstance , HINSTANCE hPrevInstance , LPSTR lpCmdLine , int nCmdShow ) {
	//int main( ) {
	find( ) ;
	return 0 ;
}
