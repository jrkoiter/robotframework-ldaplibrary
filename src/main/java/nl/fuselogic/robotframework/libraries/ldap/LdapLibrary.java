/*
 * Copyright 20013 FuseLogic BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package nl.fuselogic.robotframework.libraries.ldap;
 
import com.unboundid.ldap.sdk.LDAPConnection;
 
import com.unboundid.ldap.sdk.LDAPException;
 
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;
 
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
 
import java.io.InputStream;
 
import java.util.Arrays;
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
       
        System.out.println("*INFO* Connecting to "+host+":"+port+" as "+bindDN);
       
        ldapConnection = new LDAPConnection(host, port, bindDN, password);
    }
   
    @RobotKeyword("Fails if LDAP search does not return a single entry")
    @ArgumentNames({"baseDn", "scope", "filter"})
    public void ldapSearchShouldReturnSingleEntry(String basedn, String scope, String filter) throws LDAPSearchException {
       
        SearchResult s = this.performSearch(basedn, scope, filter, (String)null);
       
        if(s.getEntryCount() != 1) {
            throw new RuntimeException("*FAIL* LDAP search returned "+s.getEntryCount()+" entries");
        }
    }
   
    @RobotKeyword("Fails if LDAP search does not return any entries")
    @ArgumentNames({"baseDn", "scope", "filter"})
    public void ldapSearchShouldReturnEntries(String basedn, String scope, String filter) throws LDAPSearchException {
       
        SearchResult s = this.performSearch(basedn, scope, filter, (String)null);
       
        if(s.getEntryCount() == 0) {
            throw new RuntimeException("*FAIL* LDAP search did not return any entries");
        }
    }
   
    @RobotKeyword("Fails if LDAP search returns any entries")
    @ArgumentNames({"baseDn", "scope", "filter"})
    public void ldapSearchShouldNotReturnEntries(String basedn, String scope, String filter) throws LDAPSearchException {
       
        SearchResult s = this.performSearch(basedn, scope, filter, (String)null);
       
        if(s.getEntryCount() != 0) {
            throw new RuntimeException("*FAIL* LDAP search returned "+s.getEntryCount()+" entries");
        }
    }
   
    @RobotKeyword("Returns the (first) value of the specified attribute of the ldap entry identified by the search parameters. The search should only return a single ldap entry.")
    @ArgumentNames({"baseDn", "scope", "filter", "attribute"})
    public String getSingleAttributeValueFromLdapEntry(String basedn, String scope, String filter, String attribute) throws LDAPSearchException {
       
        SearchResult s = this.performSearch(basedn, scope, filter, attribute);
       
        if(s.getEntryCount() != 1) {
            throw new RuntimeException("*FAIL* LDAP search returned "+s.getEntryCount()+" entries");
        }
       
        List<SearchResultEntry> entries = s.getSearchEntries();
        String value =  entries.get(0).getAttributeValue(attribute);
       
        System.out.println("*INFO* Returning value '"+value+"'");
       
        return value;
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
       
        if(attributes == null) {
            System.out.println("*INFO* Performing search with base '"+base+"', scope '"+scope+"', filter '"+filter+"' and attributes 'null'");
        } else {
            System.out.println("*INFO* Performing search with base '"+base+"', scope '"+scope+"', filter '"+filter+"' and attributes '"+Arrays.toString(attributes)+"'");
        }
       
        return ldapConnection.search(base, scope, filter, attributes);
    }
}
