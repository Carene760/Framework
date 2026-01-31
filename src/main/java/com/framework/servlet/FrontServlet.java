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

    private Map<String, MethodRoute> routes;
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

            System.out.println("✅ Routes normales: " + routes.size());
            System.out.println("✅ Routes paramétrées: " + parameterizedRoutes.size());
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
        // 1. Chercher dans les routes exactes
        MethodRoute exactRoute = routes.get(path);
        if (exactRoute != null && exactRoute.matchesHttpMethod(httpMethod)) {
            System.out.println("✅ Route exacte trouvée: " + exactRoute);
            return exactRoute;
        }
        
        // 2. Chercher dans les routes paramétrées
        for (MethodRoute paramRoute : parameterizedRoutes) {
            if (RouteMatcher.matches(path, paramRoute.getPathPattern()) && 
                paramRoute.matchesHttpMethod(httpMethod)) {
                System.out.println("✅ Route paramétrée matchée: " + paramRoute);
                return paramRoute;
            }
        }
        
        // 3. Route non trouvée ou méthode HTTP incorrecte
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
    
    private void sendNotFound(HttpServletResponse response, String path, String method) 
            throws IOException {
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<h2>❌ Erreur 404 - Route non trouvée</h2>");
        out.println("<p>Méthode: <strong>" + method + "</strong></p>");
        out.println("<p>Chemin: <strong>" + path + "</strong></p>");
        out.println("<h3>Routes disponibles:</h3>");
        out.println("<ul>");
        
        // Afficher les routes par méthode HTTP
        Map<String, List<String>> routesByMethod = new HashMap<>();
        
        // Routes exactes
        routes.forEach((routePath, route) -> {
            String httpMethod = route.getHttpMethod();
            routesByMethod.computeIfAbsent(httpMethod, k -> new ArrayList<>())
                         .add(routePath);
        });
        
        // Routes paramétrées
        parameterizedRoutes.forEach(route -> {
            String httpMethod = route.getHttpMethod();
            routesByMethod.computeIfAbsent(httpMethod, k -> new ArrayList<>())
                         .add(route.getPathPattern() + " (paramétrée)");
        });
        
        // Afficher par méthode HTTP
        routesByMethod.forEach((httpMethod, routeList) -> {
            out.println("<li><strong>" + httpMethod + ":</strong>");
            out.println("<ul>");
            Collections.sort(routeList);
            routeList.forEach(r -> out.println("<li>" + r + "</li>"));
            out.println("</ul></li>");
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