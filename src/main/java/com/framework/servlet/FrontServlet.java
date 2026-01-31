package com.framework.servlet;

import com.framework.model.ModelView;
import com.framework.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;

public class FrontServlet extends HttpServlet {

    private Map<String, MethodRoute> routes;
    private java.util.List<MethodRoute> parameterizedRoutes;

    @Override
    public void init() throws ServletException {
        try {
            String packageControllers = getServletConfig().getInitParameter("controller-package");
            if (packageControllers == null) {
                packageControllers = "com.cousin.controller"; 
            }

            ControllerScanner scanner = new ControllerScanner(packageControllers);
            scanner.afficherLesControllersEtRoutes();

            this.routes = scanner.getRoutes();
            this.parameterizedRoutes = scanner.getParameterizedRoutes();

            System.out.println("✅ Routes normales: " + routes.size());
            System.out.println("✅ Routes paramétrées: " + parameterizedRoutes.size());
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String path = request.getRequestURI().substring(request.getContextPath().length());

        try {
            MethodRoute methodRoute = null;
            String matchedPattern = null;
            
            // 1. Chercher d'abord dans les routes exactes
            methodRoute = this.routes.get(path);
            if (methodRoute != null) {
                matchedPattern = path;
                System.out.println("✅ Route exacte trouvée: " + path);
            }
            
            // 2. Sinon chercher dans les routes paramétrées
            if (methodRoute == null && !parameterizedRoutes.isEmpty()) {
                for (MethodRoute paramRoute : parameterizedRoutes) {
                    if (RouteMatcher.matches(path, paramRoute.getPathPattern())) {
                        methodRoute = paramRoute;
                        matchedPattern = paramRoute.getPathPattern();
                        System.out.println("✅ Route paramétrée matchée: " + path + " → " + matchedPattern);
                        break;
                    }
                }
            }
            
            if (methodRoute != null) {
                Method method = methodRoute.getMethod();
                Object controller = methodRoute.getControllerInstance();
                
                // Résoudre TOUS les paramètres (URL + GET/POST)
                Map<String, Object> parameterValues = ParameterResolver.resolveAllParameters(
                    request, method, path, matchedPattern);
                
                // Préparer les arguments
                Object[] args = ParameterResolver.prepareArguments(method, parameterValues);
                
                // Afficher le débogage
                ParameterResolver.debugArguments(method, args);
                
                // Invoquer la méthode avec les arguments
                Object result = method.invoke(controller, args);

                // Traiter le résultat
                if (result instanceof String) {
                    out.println((String) result); 
                } else if (result instanceof ModelView) {
                    ModelView mv = (ModelView) result;
                    mv.getData().forEach(request::setAttribute);
                    RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getView());
                    dispatcher.forward(request, response);
                } else {
                    out.println("<h2>Type de retour non supporté: " + result.getClass().getName() + "</h2>");
                }
            } else {
                out.println("<h2>❌ Aucune route trouvée pour: " + path + "</h2>");
                out.println("<h3>Routes disponibles:</h3>");
                out.println("<ul>");
                routes.keySet().forEach(r -> out.println("<li>" + r + "</li>"));
                parameterizedRoutes.forEach(r -> out.println("<li>" + r.getPathPattern() + " (paramétrée)</li>"));
                out.println("</ul>");
            }
        } catch (Exception e) {
            out.println("<h2>❌ Erreur lors du traitement</h2>");
            out.println("<pre>");
            e.printStackTrace(out);
            out.println("</pre>");
        }
    }
}