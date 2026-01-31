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
            String matchedPattern = path; // Pour les routes exactes
            
            // 2. Si aucune route exacte, vérifier les routes paramétrées
            if (methodRoute == null && !parameterizedRoutes.isEmpty()) {
                for (MethodRoute paramRoute : parameterizedRoutes) {
                    if (RouteMatcher.matches(path, paramRoute.getPathPattern())) {
                        methodRoute = paramRoute;
                        matchedPattern = paramRoute.getPathPattern();
                        System.out.println("🔍 Route paramétrée matchée : " + path + " -> " + matchedPattern);
                        break;
                    }
                }
            }
            
            if (methodRoute != null) {
                Method method = methodRoute.getMethod();
                Object controller = methodRoute.getControllerInstance();
                
                // Résoudre les paramètres (avec support des paramètres d'URL)
                Map<String, Object> parameterValues = ParameterResolver.resolveParametersWithUrl(
                    request, method, path, matchedPattern);
                
                // Préparer les arguments
                Object[] args = ParameterResolver.prepareArguments(method, parameterValues);
                
                // Afficher le résumé
                System.out.println("🎯 Arguments pour " + method.getName() + ":");
                for (int i = 0; i < args.length; i++) {
                    System.out.println("  [" + i + "] " + method.getParameters()[i].getName() + 
                                     " = " + args[i] + 
                                     " (" + (args[i] != null ? args[i].getClass().getSimpleName() : "null") + ")");
                }
                
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
                out.println("<h2>Aucune route trouvée pour : " + path + "</h2>");
            }
        } catch (Exception e) {
            e.printStackTrace(out);
        }
    }

    private void handleResult(Object result, HttpServletRequest request, 
                            HttpServletResponse response, PrintWriter out) 
            throws ServletException, IOException {
        if (result instanceof String) {
            out.println((String) result); 
        } else if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            mv.getData().forEach(request::setAttribute);
            RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getView());
            dispatcher.forward(request, response);
        }
    }

    private void showRouteNotFound(String path, PrintWriter out) {
        out.println("<h2>❌ Aucune route trouvée pour: " + path + "</h2>");
        out.println("<h3>Routes disponibles:</h3>");
        out.println("<ul>");
        routes.keySet().forEach(r -> out.println("<li>" + r + "</li>"));
        parameterizedRoutes.forEach(r -> out.println("<li>" + r.getPathPattern() + " (paramétrée)</li>"));
        out.println("</ul>");
    }
}