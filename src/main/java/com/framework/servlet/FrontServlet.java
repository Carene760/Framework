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

            ServletContext context = getServletContext();
            context.setAttribute("controllerPackage", packageControllers);
            context.setAttribute("routes", this.routes);
            context.setAttribute("parameterizedRoutes", this.parameterizedRoutes);

            System.out.println("✅ Package contrôleur et routes enregistrés dans le ServletContext !");
            System.out.println("✅ Routes paramétrées détectées : " + parameterizedRoutes.size());
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation du framework", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String path = request.getRequestURI().substring(request.getContextPath().length());

        try {
            // 1. Vérifier d'abord les routes exactes
            MethodRoute methodRoute = this.routes.get(path);
            
            // 2. Si aucune route exacte, vérifier les routes paramétrées
            if (methodRoute == null && !parameterizedRoutes.isEmpty()) {
                for (MethodRoute paramRoute : parameterizedRoutes) {
                    if (RouteMatcher.matches(path, paramRoute.getPathPattern())) {
                        methodRoute = paramRoute;
                        System.out.println("🔍 Route paramétrée matchée : " + path + " -> " + paramRoute.getPathPattern());
                        break;
                    }
                }
            }
            
            if (methodRoute != null) {
                Method method = methodRoute.getMethod();
                Object controller = methodRoute.getControllerInstance();
                
                // Récupérer et afficher les paramètres de la requête
                Map<String, String[]> requestParams = request.getParameterMap();
                if (!requestParams.isEmpty()) {
                    System.out.println("📥 Paramètres de la requête:");
                    for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
                        System.out.println("  - " + entry.getKey() + " = " + 
                                         String.join(", ", entry.getValue()));
                    }
                }
                
                // Résoudre les paramètres de la méthode
                Map<String, Object> parameterValues = ParameterResolver.resolveParameters(request, method);
                
                // Afficher les valeurs résolues
                if (!parameterValues.isEmpty()) {
                    System.out.println("🎯 Paramètres résolus pour l'appel:");
                    for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
                        System.out.println("  - " + entry.getKey() + " = " + entry.getValue() + 
                                         " (" + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : "null") + ")");
                    }
                }
                
                // Préparer les arguments pour l'invocation
                Object[] args = ParameterResolver.prepareArguments(method, parameterValues);
                
                // Invoquer la méthode avec les arguments
                Object result = method.invoke(controller, args);

                if (result instanceof String) {
                    out.println((String) result); 
                } else if (result instanceof ModelView) {
                    ModelView mv = (ModelView) result;
                    mv.getData().forEach(request::setAttribute);
                    RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getView());
                    dispatcher.forward(request, response);
                }
            } else {
                out.println("<h2>Aucune route trouvée pour cette URL : " + path + "</h2>");
                out.println("<h3>Routes disponibles :</h3>");
                out.println("<ul>");
                routes.keySet().forEach(r -> out.println("<li>" + r + "</li>"));
                parameterizedRoutes.forEach(r -> out.println("<li>" + r.getPathPattern() + " (paramétrée)</li>"));
                out.println("</ul>");
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }
}