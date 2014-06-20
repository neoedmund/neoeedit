package neoe.ne;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

//import sun.org.mozilla.javascript.internal.NativeArray;
/**
 * use lib rhino
 */
public class JS {

    private static void addResult(Object o, List<StringBuffer> res) {
        if (o == null) {
            return;
        } else if (o instanceof String[]) {
            String[] arr = (String[]) o;
            int len = arr.length;
            for (int j = 0; j < len; j++) {
                res.add(new StringBuffer(arr[j]));
            }
        } else {
            res.add(new StringBuffer(o.toString()));
        }
    }

    public static List<StringBuffer> run_WithScriptEngineManager(List<StringBuffer> lines, String userScript) throws Exception {
        List<StringBuffer> res = new ArrayList<StringBuffer>();
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        if (engine == null) {
            System.out.println("Bad: js engine not found in JRE!");
            return null;
        }
        engine.eval(funcWrappedRun);
        engine.eval(userScript);

        Invocable jsInvoke = (Invocable) engine;

        int total = lines.size();
        for (int i = 0; i < total; i++) {
            Object o = jsInvoke.invokeFunction("wrappedRun", new Object[]{lines.get(i).toString(), i, total});
            addResult(o, res);
        }
        if (res.size() == 0) {
            res.add(new StringBuffer("no result"));
        }

        return res;

    }

    public static List<StringBuffer> run(List<StringBuffer> lines, String userScript) throws Exception {
        return run_WithRhino(lines, userScript);
    }

    static String funcWrappedRun = "function wrappedRun(a,b,c){var ret=run(a,b,c);if (ret==undefined) {return ret;} if (ret.constructor == Array){"
            + "  var jArr = java.lang.reflect.Array.newInstance(java.lang.String, ret.length);" + "  for (var i = 0; i < ret.length; i++) jArr[i] = ret[i];" + "  return jArr;"
            + "}else{return ret;}}";

    public static List<StringBuffer> run_WithRhino(List<StringBuffer> lines, String userScript) throws Exception {
        Class cxCls = null;
        Class funcCls = null;
        Class clsScriptable = null;
        try {
            cxCls = Plugin.cl.loadClass("org.mozilla.javascript.Context");
            funcCls = Plugin.cl.loadClass("org.mozilla.javascript.Function");
            clsScriptable = Plugin.cl.loadClass("org.mozilla.javascript.Scriptable");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Cannot find Rhino(org.mozilla.javascript.*), maybe you should copy js.jar into dir '<home>/.neoeedit/plugins'.");
        }

        Object cx = cxCls.getMethod("enter", new Class[]{}).invoke(null, null);
        try {

            try {

                Object scope = cxCls.getMethod("initStandardObjects", new Class[]{}).invoke(cx, null);

                Method evaluateString = findMethod1(cxCls, "evaluateString", 5);
                evaluateString.invoke(cx, new Object[]{scope, funcWrappedRun, "<1>", 1, null});
                evaluateString.invoke(cx, new Object[]{scope, userScript, "<1>", 1, null});

                Method funcCall = findMethod1(funcCls, "call", 4);
                Object funcObj = clsScriptable.getMethod("get", new Class[]{String.class, clsScriptable})
                        .invoke(scope, new Object[]{"wrappedRun", scope});

                List<StringBuffer> res = new ArrayList<StringBuffer>();
                int total = lines.size();
                for (int i = 0; i < total; i++) {
                    Object o = funcCall.invoke(funcObj, new Object[]{cx, scope, scope, new Object[]{lines.get(i).toString(), i, total}});
                    addResult(o, res);
                }
                if (res.size() == 0) {
                    res.add(new StringBuffer("no result"));
                }
                return res;
            } catch (Exception ex) {
                Throwable upper = ex.getCause();
                if (upper == null) {
                    throw ex;
                } else {
                    // javascript error
                    throw (Exception) upper;
                }
            }

        } finally {
            // Exit from the context.
            //Context.exit();
            cxCls.getMethod("exit", new Class[]{}).invoke(null, null);

        }

    }

    /**caution: maybe found the wrong method, confirm has only 1 such method*/
    private static Method findMethod1(Class cls, String name, int paramCnt) {
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(name) && (m.getParameterTypes().length == paramCnt)) {
                return m;
            }
        }
        return null;
    }
}
