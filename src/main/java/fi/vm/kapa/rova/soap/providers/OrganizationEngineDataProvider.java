package fi.vm.kapa.rova.soap.providers;

import java.util.List;

import javax.xml.ws.Holder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.xml.rova.api.authorization.RovaAuthorizationResponse;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationListType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationalRolesType;
import fi.vm.kapa.xml.rova.api.orgroles.RoleList;

@Component
public class OrganizationEngineDataProvider implements OrganizationDataProvider, SpringProperties {

    Logger LOG = Logger.getLogger(OrganizationEngineDataProvider.class, Logger.SOAP_SERVICE);

    private fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory organizationalRolesFactory = new fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory();
  
    @Value(REQUEST_ALIVE_SECONDS)
    private Integer requestAliveSeconds;

    public void handleOrganizationalRoles(String personId, List<String> organizationIds, String service,
            String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> responseHolder) {
     
        if (responseHolder.value == null) {
            responseHolder.value = organizationalRolesFactory.createResponse();
        }
        
        OrganizationListType organizationListType = organizationalRolesFactory.createOrganizationListType();

        responseHolder.value.setOrganizationList(organizationListType);
        
        List<OrganizationalRolesType> organizationalRoles = organizationListType.getOrganization();
        OrganizationalRolesType roleType=new OrganizationalRolesType();
        if (organizationIds!=null && organizationIds.size()>0 && organizationIds.get(0).equals("Y98765")) {
            OrganizationType organizationType = organizationalRolesFactory.createOrganizationType();
            RoleList roleList = organizationalRolesFactory.createRoleList();
            organizationType.setOrganizationIdentifier("Y98765");
            organizationType.setName("Testilafka Oy");
            roleList.getRole().add("TJ");
            roleType.setOrganization(organizationType);
            roleType.setRoles(roleList);
            organizationalRoles.add(roleType);
        } else {
            OrganizationType organizationType = organizationalRolesFactory.createOrganizationType();
            RoleList roleList = organizationalRolesFactory.createRoleList();
            organizationType.setOrganizationIdentifier("Y12345678");
            organizationType.setName("Kesäpojjaat Oy");
            roleList.getRole().add("TJ");
            roleList.getRole().add("HPJ");
            roleType.setOrganization(organizationType);
            roleType.setRoles(roleList);
            organizationalRoles.add(roleType);
        }
    }
    
    @Override
    public String toString() {
        return "OrganizationalEngineDataProvider";
    }

}
