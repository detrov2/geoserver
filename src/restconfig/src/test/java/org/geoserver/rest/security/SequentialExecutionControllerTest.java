/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.rest.security.xml.JaxbRoleList;
import org.geoserver.rest.security.xml.JaxbUser;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class SequentialExecutionControllerTest extends GeoServerTestSupport {

    private static final String USER_SERVICE = "default";

    protected UsersRestController usersController;

    protected RolesRestController rolesController;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        usersController = applicationContext.getBean(UsersRestController.class);
        rolesController = applicationContext.getBean(RolesRestController.class);
    }

    @Test
    public void testMultipleRolesAttachment() throws PasswordPolicyException, IOException {
        JaxbUser user = new JaxbUser();
        user.setUserName("popo");
        user.setPassword("secret");
        user.setEnabled(true);
        usersController.insertUser(USER_SERVICE, user);

        List<String> roles = new ArrayList<>();
        roles.add("rolea");
        roles.add("roleb");
        roles.add("rolec");
        roles.add("roled");

        for (String role : roles) {
            try {
                rolesController.insert(role);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<String> requestUrls = new ArrayList<>();
        roles.forEach(role -> requestUrls.add("rest/security/roles/role/" + role + "/user/popo"));

        requestUrls
                .parallelStream()
                .forEach(
                        requestUrl -> {
                            MockHttpServletRequest request = createRequest(requestUrl);
                            request.setMethod("POST");

                            try {
                                dispatch(request, "UTF-8");
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

        JaxbRoleList jaxbRoleList = rolesController.getUser("popo");
        assertEquals(4, jaxbRoleList.getRoles().size());
        assertTrue(jaxbRoleList.getRoles().contains("rolea"));
        assertTrue(jaxbRoleList.getRoles().contains("roleb"));
        assertTrue(jaxbRoleList.getRoles().contains("rolec"));
        assertTrue(jaxbRoleList.getRoles().contains("roled"));
    }
}
