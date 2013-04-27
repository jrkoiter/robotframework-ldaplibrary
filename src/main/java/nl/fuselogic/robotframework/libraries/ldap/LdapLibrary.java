package nl.fuselogic.robotframework.libraries.ldap;

import com.unboundid.ldap.sdk.LDAPConnection;

import com.unboundid.ldap.sdk.LDAPException;

import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;

import com.unboundid.ldap.sdk.SearchScope;

import java.io.InputStream;

import java.util.List;

import java.util.Scanner;

import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.robotframework.javalib.library.AnnotationLibrary;

@RobotKeywords
public class LdapLibrary extends AnnotationLibrary {
    
    public static final String ROBOT_LIBRARY_VERSION = "0.2";
    
    private static LDAPConnection ldapConnection;
    
    public LdapLibrary(List<String> list) {
        super(list);
    }

    public LdapLibrary(String string) {
        super(string);
    }

    public LdapLibrary() {
        super("nl/fuselogic/robotframework/libraries/ldap/*.class");
    }
    
    @Override
    public String getKeywordDocumentation(String keywordName) {
        if (keywordName.equals("__intro__") || keywordName.equals("__init__")) {
            InputStream in = this.getClass().getResourceAsStream(keywordName + ".txt");
            Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
        return super.getKeywordDocumentation(keywordName);
    }
    
    @RobotKeyword("Makes a connection to specified LDAP server")
    @ArgumentNames({"host", "port", "bindDN", "password"})
    public void connectToLdap(String host, Integer port, String bindDN, String password) throws LDAPException {
        if(ldapConnection != null && ldapConnection.isConnected()) {
            System.out.println("*WARN* There is already an LDAP connection open");
            return;
        }
        
        ldapConnection = new LDAPConnection(host, port, bindDN, password);
    }
    
    @RobotKeyword("Fails if LDAP search does not return any entries")
    @ArgumentNames({"baseDn", "scope", "filter"})
    public void ldapSearchShouldReturnEntries(String basedn, String scope, String filter) throws LDAPSearchException {
        
        SearchResult s = this.performSearch(basedn, scope, filter, (String)null);
        
        if(s.getEntryCount() == 0) {
            throw new RuntimeException("*FAIL* LDAP search did not return any entries ('"+basedn+"','"+scope+"','"+filter+"')");
        }
    }
    
    @RobotKeyword("Fails if LDAP search returns any entries")
    @ArgumentNames({"baseDn", "scope", "filter"})
    public void ldapSearchShouldNotReturnEntries(String basedn, String scope, String filter) throws LDAPSearchException {
        
        SearchResult s = this.performSearch(basedn, scope, filter, (String)null);
        
        if(s.getEntryCount() != 0) {
            throw new RuntimeException("*FAIL* LDAP search returned "+s.getEntryCount()+" entries ('"+basedn+"','"+scope+"','"+filter+"')");
        }
    }
    
    @RobotKeyword("Disconnects from LDAP server")
    public void disconnectFromLdap() {
        if(ldapConnection == null || !ldapConnection.isConnected()) {
            System.out.println("*WARN* There is no current connection to an LDAP server");
            return;
        }
        
        ldapConnection.close();
        ldapConnection = null;
    }
    
    private SearchResult performSearch (String base, String scopeStr, String filter, java.lang.String... attributes) throws LDAPSearchException {
        if (ldapConnection == null) {
            throw new RuntimeException("*EXCEPTION* There is no LDAP connection open");
        }
        
        SearchScope scope;
            
        if(scopeStr.equalsIgnoreCase("BASE")) {
            scope = SearchScope.BASE;
        } else if(scopeStr.equalsIgnoreCase("ONE")) {
            scope = SearchScope.ONE;
        } else if(scopeStr.equalsIgnoreCase("SUB")) {
            scope = SearchScope.SUB;
        } else {
            throw new RuntimeException("*EXCEPTION* scope should be BASE, ONE or SUB.");
        }
        
        if (ldapConnection == null) {
            throw new RuntimeException("*EXCEPTION* There is no LDAP connection open");
        }
        
        return ldapConnection.search(base, scope, filter, attributes);
    }
}
