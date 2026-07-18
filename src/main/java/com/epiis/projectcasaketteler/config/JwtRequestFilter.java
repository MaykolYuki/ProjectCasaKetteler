package com.epiis.projectcasaketteler.config;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.epiis.projectcasaketteler.entity.EntityAdmin;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.JwtHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAdmin;
import com.epiis.projectcasaketteler.repository.RepositoryUser;
import com.epiis.projectcasaketteler.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private RepositoryUser repositoryUser;

    @Autowired
    private RepositoryAdmin repositoryAdmin;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                email = jwtHelper.extractEmail(jwt);
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"type\":\"error\",\"message\":\"Token expirado. Inicie sesión nuevamente.\"}");
                return;
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"type\":\"error\",\"message\":\"Token inválido.\"}");
                return;
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            if (!esTokenVigente(email, jwt)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"type\":\"error\",\"message\":\"Sesión invalidada. Inicie sesión nuevamente.\"}");
                return;
            }

            UserDetails userDetails;
            try {
                userDetails = this.userDetailsService.loadUserByUsername(email);
            } catch (Exception e) {
                // antes esto no estaba protegido: una cuenta desactivada tiraba una
                // excepción sin capturar en vez de un 401 limpio
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
                return;
            }

            if (jwtHelper.validateToken(jwt, email)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        chain.doFilter(request, response);
    }

    private boolean esTokenVigente(String email, String jwt) {
        Date tokenValidAfter = null;

        Optional<EntityUser> userOpt = repositoryUser.findByEmail(email);
        if (userOpt.isPresent()) {
            tokenValidAfter = userOpt.get().getTokenValidAfter();
        } else {
            Optional<EntityAdmin> adminOpt = repositoryAdmin.findByEmail(email);
            if (adminOpt.isPresent()) {
                tokenValidAfter = adminOpt.get().getTokenValidAfter();
            }
        }

        if (tokenValidAfter == null) {
            return true; // esta cuenta nunca invalidó tokens anteriores
        }

        Date issuedAt = jwtHelper.extractIssuedAt(jwt);
        return issuedAt.after(tokenValidAfter);
    }
}