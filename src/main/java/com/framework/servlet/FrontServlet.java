package com.framework.servlet;

import com.framework.model.ModelView;
import com.framework.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.*;

public class FrontServlet extends HttpServlet {

    private Map<String, RouteEntry> routes;
    private List<MethodRoute> parameterizedRoutes;

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

            System.out.println("✅ Entrées de route: " + routes.size());
            System.out.println("✅ Routes paramétrées: " + parameterizedRoutes.size());
            
            // Debug: afficher toutes les routes
            System.out.println("📋 Toutes les routes enregistrées:");
            routes.forEach((path, entry) -> {
                System.out.println("  " + path + " -> " + entry.getAllMethods().keySet());
            });
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation", e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod(); 
        
        System.out.println("\n=== Nouvelle requête ===");
        System.out.println("🌐 " + method + " " + path);

        try {
            MethodRoute methodRoute = findRoute(path, method);
            
            if (methodRoute != null) {
                processRoute(request, response, methodRoute, path);
            } else {
                sendNotFound(response, path, method);
            }
        } catch (Exception e) {
            sendError(response, e);
        }
    }
    
    private MethodRoute findRoute(String path, String httpMethod) {
        System.out.println("🔍 Recherche de route pour: " + httpMethod + " " + path);
        
        // 1. Chercher dans les routes exactes (avec RouteEntry)
        RouteEntry routeEntry = routes.get(path);
        if (routeEntry != null) {
            System.out.println("🎯 RouteEntry trouvée pour: " + path);
            System.out.println("🎯 Méthodes disponibles: " + routeEntry.getAllMethods().keySet());
            
            MethodRoute methodRoute = routeEntry.getMethod(httpMethod);
            if (methodRoute != null) {
                System.out.println("✅ Route exacte trouvée: " + methodRoute);
                return methodRoute;
            } else {
                System.out.println("⚠️ Chemin trouvé mais méthode " + httpMethod + " non disponible");
                System.out.println("⚠️ Méthodes disponibles: " + routeEntry.getAllMethods().keySet());
            }
        } else {
            System.out.println("❌ Aucune RouteEntry pour: '" + path + "'");
        }
        
        // 2. Chercher dans les routes paramétrées
        System.out.println("🔍 Recherche dans " + parameterizedRoutes.size() + " routes paramétrées...");
        for (MethodRoute paramRoute : parameterizedRoutes) {
            if (RouteMatcher.matches(path, paramRoute.getPathPattern())) {
                if (paramRoute.matchesHttpMethod(httpMethod)) {
                    System.out.println("✅ Route paramétrée matchée: " + paramRoute);
                    return paramRoute;
                } else {
                    System.out.println("⚠️ Route paramétrée matchée mais méthode incorrecte: " + 
                                     paramRoute.getHttpMethod() + " != " + httpMethod);
                }
            }
        }
        
        // 3. Route non trouvée
        System.out.println("❌ Aucune route trouvée pour " + httpMethod + " " + path);
        return null;
    }
    
    private void processRoute(HttpServletRequest request, HttpServletResponse response,
                             MethodRoute methodRoute, String path) throws Exception {
        
        Method method = methodRoute.getMethod();
        Object controller = methodRoute.getControllerInstance();
        
        // Résoudre les paramètres
        Map<String, Object> parameterValues = ParameterResolver.resolveAllParameters(
            request, method, path, 
            methodRoute.getPathPattern() != null ? methodRoute.getPathPattern() : path
        );
        
        // Préparer les arguments
        Object[] args = ParameterResolver.prepareArguments(method, parameterValues);
        
        // Afficher le débogage
        ParameterResolver.debugArguments(method, args);
        
        // Invoquer la méthode
        Object result = method.invoke(controller, args);
        
        // Traiter le résultat
        handleResult(request, response, result);
    }
    
    private void handleResult(HttpServletRequest request, HttpServletResponse response, Object result) 
            throws ServletException, IOException {
        
        PrintWriter out = response.getWriter();
        response.setContentType("text/html;charset=UTF-8");
        
        if (result instanceof String) {
            out.println((String) result); 
        } else if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            mv.getData().forEach(request::setAttribute);
            RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getView());
            dispatcher.forward(request, response);
        } else if (result == null) {
            out.println("<h2>Aucun retour de la méthode</h2>");
        } else {
            out.println("<h2>Type de retour non supporté: " + result.getClass().getName() + "</h2>");
        }
    }
    
    private void sendNotFound(HttpServletResponse response, String path, String method) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<h2>❌ Erreur 404 - Route non trouvée</h2>");
        out.println("<p>Méthode: <strong>" + method + "</strong></p>");
        out.println("<p>Chemin: <strong>" + path + "</strong></p>");
        
        out.println("<h3>Routes disponibles pour ce chemin:</h3>");
        RouteEntry entry = routes.get(path);
        if (entry != null) {
            out.println("<ul>");
            entry.getAllMethods().forEach((httpMethod, route) -> {
                out.println("<li><strong>" + httpMethod + ":</strong> " + 
                          route.getMethod().getDeclaringClass().getSimpleName() + "." + 
                          route.getMethod().getName() + "()</li>");
            });
            out.println("</ul>");
        } else {
            out.println("<p>Aucune route pour ce chemin exact</p>");
        }
        
        out.println("<h3>Toutes les routes disponibles:</h3>");
        out.println("<ul>");
        routes.forEach((routePath, routeEntry) -> {
            out.println("<li><strong>" + routePath + ":</strong> " + 
                      routeEntry.getAllMethods().keySet() + "</li>");
        });
        out.println("</ul>");
        
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
    
    private void sendError(HttpServletResponse response, Exception e) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<h2>❌ Erreur interne du serveur</h2>");
        out.println("<pre>");
        e.printStackTrace(out);
        out.println("</pre>");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}