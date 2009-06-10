package org.apache.felix.framework.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class JarHandler
{
    
    private JarInputStream _jarInputStream;
    private HashMap _entriesByName= new HashMap();
    private HashMap _contentsByName= new HashMap();
    private Manifest _manifest; 
    
    public JarHandler(JarInputStream jarInputStream)
    {
        _jarInputStream= jarInputStream;
    }

    public void close()
    throws IOException
    {
        _jarInputStream.close();
    }

    public ZipEntry getEntry( String name ) 
    throws IOException
    {
        ZipEntry entry = findEntry( name );
        if ((entry != null) && (entry.getSize() == 0) && !entry.isDirectory())
        {
            ZipEntry dirEntry = findEntry(name + '/');
            if (dirEntry != null)
            {
                entry = dirEntry;
            }
        }
        return entry;
    }
    
    private ZipEntry findEntry( String name ) throws IOException
    {
        ZipEntry entry = (ZipEntry)_entriesByName.get(name);
        ZipEntry zipEntry;
        while ((zipEntry =readEntry()) != null && entry == null )
        {
            String entryName= zipEntry.getName();
            _entriesByName.put( entryName, zipEntry );
            if (entryName.equals( name ))
                entry= zipEntry;
        }
        return entry;
    }
    
    private ZipEntry readEntry() throws IOException
    {
        ZipEntry zipEntry=_jarInputStream.getNextEntry();
        if (zipEntry != null)
        {
            String entryName= zipEntry.getName();
            _entriesByName.put( entryName, zipEntry );
            
            ByteArrayOutputStream out= new ByteArrayOutputStream();
            int size= (int)zipEntry.getSize();
            if (Integer.MAX_VALUE < zipEntry.getSize() || size <= 0)
                size= 1024*64;
            byte[] buffer= new byte[size];
            int count;
            while ((count= _jarInputStream.read( buffer )) != -1)
                out.write( buffer, 0, count );
            _contentsByName.put( entryName, out.toByteArray() );
        }
        return zipEntry;
    }
    
    public Enumeration entries() throws IOException
    {
        ZipEntry zipEntry;
        while ((zipEntry =readEntry()) != null)
        {
            String entryName= zipEntry.getName();
            _entriesByName.put( entryName, zipEntry );
        }
        return Collections.enumeration( _entriesByName.values() );
    }

    public InputStream getInputStream( ZipEntry ze )
    {
        return new ByteArrayInputStream((byte[])_contentsByName.get(ze.getName()));
    }

    public Manifest getManifest()
    {
        if (_manifest == null)
            _manifest= _jarInputStream.getManifest();
        return _manifest;
    }

}
