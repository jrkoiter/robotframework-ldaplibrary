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
 
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
 
import com.unboundid.ldap.sdk.LDAPException;
 
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;
 
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
 
import java.io.InputStream;
import java.security.GeneralSecurityException;
 
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
 
import java.util.Scanner;
import javax.net.ssl.SSLSocketFactory;
 
import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.robotframework.javalib.library.AnnotationLibrary;
 
@RobotKeywords
public class LdapLibrary extends AnnotationLibrary {
   
    public static final String ROBOT_LIBRARY_VERSION = "0.2";
   
    private static LDAPConnection ldapConnection;
    
    private static SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
    private static SSLSocketFactory sslSocketFactory;
   
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
    
    @RobotKeyword("Makes a connection to specified LDAP server. Prefix _host_ with 'ldaps://' to make an SSL connection.")
    @ArgumentNames({"host", "port", "bindDN", "password"})
    public void connectToLdap(String host, Integer port, String bindDN, String password) throws LDAPException, GeneralSecurityException {
        
        if(!host.startsWith("ldap://") && !host.startsWith("ldaps://")) {
            host = "ldap://" + host;
        }
        
        String connectionName = host+":"+port+" as "+bindDN;
        
        if(ldapConnection != null) {
            if (!ldapConnection.getConnectionName().equals(connectionName)) {
                System.out.println("*WARN* There is already a connection to "+ldapConnection.getConnectionName()+". Going to reconnect to "+connectionName+".");
                
            } else if(ldapConnection.isConnected()) {
                System.out.println("*INFO* There is already an LDAP connection open");
                return;
            }
            ldapConnection.close();
        }
        
        if(host.startsWith("ldaps://")) {
            if(sslSocketFactory == null ) {
                sslSocketFactory = sslUtil.createSSLSocketFactory();
            }
            ldapConnection = new LDAPConnection(sslSocketFactory);
        } else {
            ldapConnection = new LDAPConnection();
        }
        
        ldapConnection.setConnectionName(connectionName);
        
        System.out.println("*INFO* Connecting to "+connectionName);
        
        ldapConnection.connect(host.substring(host.lastIndexOf("/")+1), port);
        ldapConnection.bind(bindDN, password);
    }
    
    @RobotKeyword("Returns the attributes of a single LDAP entry.\n\n" +
                    "If one ore more attribute names are specified, only those attributes will be returned. Otherwise all attributes will be returned.\n\n" + 
                    "Example:\n" +
                    "| ${entry}= | Get Ldap Entry | o=mydomain,c=com | SUB | uid=john | uid | mailAlternateAddress |\n" +
                    "| @{uid}= | Get From Dictionary | ${entry} | uid | #Check a single value attribute |\n" +
                    "| Should Be Equal | @{uid} |  john |\n" +
                    "| @{mailAlternateAddress}= | Get From Dictionary | ${entry} | mailAlternateAddress | #Check a multi value attribute |\n" +
                    "| Length Should Be | ${mailAlternateAddress} | 3 |\n" +
                    "| List Should Contain Value | ${mailAlternateAddress} | alias1@mydomain.com |\n" +
                    "| List Should Contain Value | ${mailAlternateAddress} | alias2@mydomain.com |\n" +
                    "| List Should Contain Value | ${mailAlternateAddress} | alias3@mydomain.com |")
    @ArgumentNames({"baseDn", "scope", "filter", "*attributes"})
    public HashMap<String, Object> getLdapEntry(String basedn, String scope, String filter, String[] attributes) throws LDAPSearchException, LDAPException {
        
        if(attributes == null) {
            attributes = new String[] {"*"};
        }
        
        SearchResult s = this.performSearch(basedn, scope, filter, attributes);
       
        if(s.getEntryCount() != 1) {
            throw new RuntimeException("LDAP search returned "+s.getEntryCount()+" entries");
        }
        
        HashMap<String, Object> entryMap = new HashMap<String, Object>();
        
        SearchResultEntry entry = s.getSearchEntries().get(0);
        
        Collection<Attribute> entryAttributes = entry.getAttributes();
        for(Attribute attribute : entryAttributes) {
            entryMap.put(attribute.getBaseName(), attribute.getValues());
        }
        
        return entryMap;
    }
    
    @RobotKeyword("Fails if LDAP search does not return a single entry")
    @ArgumentNames({"baseDn", "scope", "filter"})
    public void ldapSearchShouldReturnSingleEntry(String basedn, String scope, String filter) throws LDAPSearchException {
       
        SearchResult s = this.performSearch(basedn, scope, filter, (String)null);
       
        if(s.getEntryCount() != 1) {
            throw new RuntimeException("LDAP search returned "+s.getEntryCount()+" entries");
        }
    }
   
    @RobotKeyword("Fails if LDAP search does not return any entries")
    @ArgumentNames({"baseDn", "scope", "filter"})
    public void ldapSearchShouldReturnEntries(String basedn, String scope, String filter) throws LDAPSearchException {
       
        SearchResult s = this.performSearch(basedn, scope, filter, (String)null);
        
        if(s.getEntryCount() == 0) {
            throw new RuntimeException("LDAP search did not return any entries");
        }
    }
   
    @RobotKeyword("Fails if LDAP search returns any entries")
    @ArgumentNames({"baseDn", "scope", "filter"})
    public void ldapSearchShouldNotReturnEntries(String basedn, String scope, String filter) throws LDAPSearchException {
       
        SearchResult s = this.performSearch(basedn, scope, filter, (String)null);
       
        if(s.getEntryCount() != 0) {
            throw new RuntimeException("LDAP search returned "+s.getEntryCount()+" entries");
        }
    }
   
    @RobotKeyword("Returns the (first) value of the specified attribute of the ldap entry identified by the search parameters. The search should only return a single ldap entry.")
    @ArgumentNames({"baseDn", "scope", "filter", "attribute"})
    public String getSingleAttributeValueFromLdapEntry(String basedn, String scope, String filter, String attribute) throws LDAPSearchException {
       
        SearchResult s = this.performSearch(basedn, scope, filter, attribute);
       
        if(s.getEntryCount() != 1) {
            throw new RuntimeException("LDAP search returned "+s.getEntryCount()+" entries");
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
            throw new RuntimeException("There is no LDAP connection open");
        }
       
        SearchScope scope;
           
        if(scopeStr.equalsIgnoreCase("BASE")) {
            scope = SearchScope.BASE;
        } else if(scopeStr.equalsIgnoreCase("ONE")) {
            scope = SearchScope.ONE;
        } else if(scopeStr.equalsIgnoreCase("SUB")) {
            scope = SearchScope.SUB;
        } else {
            throw new RuntimeException("Scope should be BASE, ONE or SUB.");
        }
       
        if (ldapConnection == null) {
            throw new RuntimeException("There is no LDAP connection open");
        }
       
        if(attributes == null) {
            System.out.println("*INFO* Performing search with base '"+base+"', scope '"+scope+"', filter '"+filter+"' and attributes 'null'");
        } else {
            System.out.println("*INFO* Performing search with base '"+base+"', scope '"+scope+"', filter '"+filter+"' and attributes '"+Arrays.toString(attributes)+"'");
        }
       
        return ldapConnection.search(base, scope, filter, attributes);
    }
}
