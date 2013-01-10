#include <Windows.h>
#include <string>
#include <process.h>

#define BUFFER 10240
using namespace std;

inline std::wstring &
	replaceAll(std::wstring &s, const std::wstring &sub,
	const std::wstring &other)
{
	size_t b = 0;
	for (;;)
	{
		b = s.find(sub, b);
		if (b == s.npos) break;
		s.replace(b, sub.size(), other);
		b += other.size();
	}
	return s;
}



int WINAPI wWinMain(HINSTANCE hInstance,
	HINSTANCE hPrevInstance,
	LPWSTR lpCmdLine,
	int nCmdShow)
{

	LPWSTR *argv;
	int argc;

	argv = CommandLineToArgvW(GetCommandLineW(), &argc);


	WCHAR* JARFN=L"neoeedit.jar";
	WCHAR value[BUFFER];
	DWORD BufferSize = BUFFER;
	LONG  ret = RegGetValue(HKEY_CLASSES_ROOT, (LPCWSTR)L"\\jarfile\\shell\\open\\command", 
		(LPCWSTR)"", RRF_RT_ANY, NULL, (PVOID)&value, &BufferSize);
	printf("%ws\n",value);
	wstring cmd = value;
	ret = GetModuleFileName(NULL, (LPWSTR)&value, BufferSize);
	printf("%ws\n",value);
	wstring exepath = value;
	size_t p = exepath.find_last_of(L"\\");
	if (p!=string::npos){
		exepath=exepath.substr(0,p+1)+JARFN;
	}
	replaceAll(cmd, L"\"%1\"", L"");

	wstring params=L"";
	for (int i=1;i<argc;i++){
		params = params + L" \""+ argv[i] + L"\"";
	}

	replaceAll(cmd, L"%*", L"");
	replaceAll(cmd, L" -jar ", L"");	

	wstring allstr =  cmd + L" -Dfile.encoding=unicode -Xmx1000M -jar "+exepath;
	if (params.size()>0) allstr+=params;
	printf("%ws\n",allstr.c_str());
	
	/*FILE*  pPipe =  _wpopen(allstr.c_str(), L"r");
	char   psBuffer[128];
	while(fgets(psBuffer, 128, pPipe))
	{
	  printf(psBuffer);
	}
	*/
	//printf("\n%d\n",errno );
	//_wsystem(allstr.c_str());
	//_wspawnl(_P_WAIT, cmd.c_str(), L"-jar",exepath);
	STARTUPINFOW        siStartupInfo;
    PROCESS_INFORMATION piProcessInfo;
	memset(&siStartupInfo, 0, sizeof(siStartupInfo));
    memset(&piProcessInfo, 0, sizeof(piProcessInfo)); 
	CreateProcessW(NULL, (LPWSTR)allstr.c_str(), NULL, NULL, false, CREATE_DEFAULT_ERROR_MODE, NULL, NULL, &siStartupInfo, &piProcessInfo);
}
