package com.framework.servlet;

import com.framework.model.ModelView;
import com.framework.model.SessionModelView;
import com.framework.model.UserSession;
import com.framework.util.*;
import com.framework.annotation.Auth;
import com.framework.annotation.Json;
import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.*;

@MultipartConfig
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

            System.out.println(" Entres de route: " + routes.size());
            System.out.println(" Routes paramtres: " + parameterizedRoutes.size());
            
            // Debug: afficher toutes les routes
            System.out.println(" Toutes les routes enregistres:");
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
        
        System.out.println("\n=== Nouvelle requte ===");
        System.out.println(" " + method + " " + path);

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
        System.out.println(" Recherche de route pour: " + httpMethod + " " + path);
        
        // 1. Chercher dans les routes exactes (avec RouteEntry)
        RouteEntry routeEntry = routes.get(path);
        if (routeEntry != null) {
            System.out.println(" RouteEntry trouve pour: " + path);
            System.out.println(" Mthodes disponibles: " + routeEntry.getAllMethods().keySet());
            
            MethodRoute methodRoute = routeEntry.getMethod(httpMethod);
            if (methodRoute != null) {
                System.out.println(" Route exacte trouve: " + methodRoute);
                return methodRoute;
            } else {
                System.out.println(" Chemin trouv mais mthode " + httpMethod + " non disponible");
                System.out.println(" Mthodes disponibles: " + routeEntry.getAllMethods().keySet());
            }
        } else {
            System.out.println(" Aucune RouteEntry pour: '" + path + "'");
        }
        
        // 2. Chercher dans les routes paramtres
        System.out.println(" Recherche dans " + parameterizedRoutes.size() + " routes paramtres...");
        for (MethodRoute paramRoute : parameterizedRoutes) {
            if (RouteMatcher.matches(path, paramRoute.getPathPattern())) {
                if (paramRoute.matchesHttpMethod(httpMethod)) {
                    System.out.println(" Route paramtre matche: " + paramRoute);
                    return paramRoute;
                } else {
                    System.out.println(" Route paramtre matche mais mthode incorrecte: " + 
                                     paramRoute.getHttpMethod() + " != " + httpMethod);
                }
            }
        }
        
        // 3. Route non trouve
        System.out.println(" Aucune route trouve pour " + httpMethod + " " + path);
        return null;
    }
    
    private void processRoute(HttpServletRequest request, HttpServletResponse response,
                             MethodRoute methodRoute, String path) throws Exception {
        
        Method method = methodRoute.getMethod();
        Object controller = methodRoute.getControllerInstance();

        Auth auth = resolveAuthAnnotation(method);
        if (auth != null) {
            UserSession userSession = AuthSessionUtil.resolveUserSession(request, getServletConfig());
            if (!userSession.isAuthenticated()) {
                sendAuthError(response, method, HttpServletResponse.SC_UNAUTHORIZED,
                        "Authentication required");
                return;
            }

            if (!AuthSessionUtil.isAuthorized(auth, userSession)) {
                sendAuthError(response, method, HttpServletResponse.SC_FORBIDDEN,
                        "Access denied for current role");
                return;
            }
        }
        
        // Rsoudre les paramtres
        Map<String, Object> parameterValues = ParameterResolver.resolveAllParameters(
            request, method, path, 
            methodRoute.getPathPattern() != null ? methodRoute.getPathPattern() : path
        );
        
        // Prparer les arguments
        Object[] args = ParameterResolver.prepareArguments(method, parameterValues);
        
        // Afficher le dbogage
        ParameterResolver.debugArguments(method, args);
        
        // Invoquer la mthode
        Object result = method.invoke(controller, args);

        // Traiter le rsultat (passer la mthode pour savoir si elle est annote @Json)
        handleResult(request, response, result, method);
    }
    
    private void handleResult(HttpServletRequest request, HttpServletResponse response, Object result, Method invokedMethod) 
            throws ServletException, IOException {

        // Si la mthode est annote @Json, rpondre en JSON
        if (invokedMethod.isAnnotationPresent(com.framework.annotation.Json.class)) {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            try {
                // Construire la structure demande
                Map<String, Object> wrapper = new LinkedHashMap<>();
                if (result == null) {
                    wrapper.put("status", "error");
                    wrapper.put("code", 404);
                    wrapper.put("data", null);
                } else {
                    wrapper.put("status", "success");
                    wrapper.put("code", 200);
                    if (result instanceof java.util.Collection) {
                        Collection<?> col = (Collection<?>) result;
                        wrapper.put("count", col.size());
                        wrapper.put("data", result);
                    } else if (result.getClass().isArray()) {
                        int len = java.lang.reflect.Array.getLength(result);
                        wrapper.put("count", len);
                        wrapper.put("data", result);
                    } else {
                        wrapper.put("data", result);
                    }
                }

                String json = com.framework.util.JsonSerializer.toJson(wrapper);
                out.println(json);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                PrintWriter outErr = response.getWriter();
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("status", "error");
                err.put("code", 500);
                err.put("data", e.getMessage());
                outErr.println(com.framework.util.JsonSerializer.toJson(err));
            }
            return;
        }

        // Comportement existant pour les autres types de rponse
        PrintWriter out = response.getWriter();
        response.setContentType("text/html;charset=UTF-8");

        if (result instanceof String) {
            out.println((String) result); 
        } else if (result instanceof SessionModelView) {
            // Grer SessionModelView: ajouter les attributs de session puis forward
            SessionModelView smv = (SessionModelView) result;
            
            // Ajouter les attributs de requte
            smv.getData().forEach(request::setAttribute);
            
            // Ajouter les attributs de session
            if (smv.hasSessionAttributes()) {
                HttpSession session = request.getSession(true);
                smv.getSessionAttributes().forEach((key, value) -> {
                    session.setAttribute(key, value);
                    System.out.println("[SESSION] " + key + " = " + value);
                });
            }
            
            RequestDispatcher dispatcher = request.getRequestDispatcher(smv.getView());
            dispatcher.forward(request, response);
        } else if (result instanceof ModelView) {
            ModelView mv = (ModelView) result;
            mv.getData().forEach(request::setAttribute);
            RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getView());
            dispatcher.forward(request, response);
        } else if (result == null) {
            out.println("<h2>Aucun retour de la mthode</h2>");
        } else {
            out.println("<h2>Type de retour non support: " + result.getClass().getName() + "</h2>");
        }
    }
    
    private void sendNotFound(HttpServletResponse response, String path, String method) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        out.println("<h2> Erreur 404 - Route non trouve</h2>");
        out.println("<p>Mthode: <strong>" + method + "</strong></p>");
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
        
        out.println("<h2> Erreur interne du serveur</h2>");
        out.println("<pre>");
        e.printStackTrace(out);
        out.println("</pre>");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private Auth resolveAuthAnnotation(Method method) {
        if (method.isAnnotationPresent(Auth.class)) {
            return method.getAnnotation(Auth.class);
        }
        Class<?> controllerClass = method.getDeclaringClass();
        if (controllerClass.isAnnotationPresent(Auth.class)) {
            return controllerClass.getAnnotation(Auth.class);
        }
        return null;
    }

    private void sendAuthError(HttpServletResponse response, Method method, int code, String message) throws IOException {
        response.setStatus(code);
        if (method.isAnnotationPresent(Json.class)) {
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "error");
            err.put("code", code);
            err.put("data", message);
            response.getWriter().println(JsonSerializer.toJson(err));
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<h2>Acces refuse</h2>");
        out.println("<p>" + message + "</p>");
        out.println("<p>Code: <strong>" + code + "</strong></p>");
    }
}


