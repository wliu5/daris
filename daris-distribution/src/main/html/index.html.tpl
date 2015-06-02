<html>
<head>
<title>DaRIS v@VERSION@</title>
<style>
body {font-family:"Lucida Grande",Helvetica,Arial,Verdana,sans-serif; line-height: 1.5em; margin-top:30px;}
div.nav {height:50px; width:100%; line-height:50px; display:block; position:absolute; top:0; left:0; color:#eee; background: rgba(0,0,0,0.6); font-size:15pt;}
a.nav:link {text-decoration:none; color:#eee;}
a.nav:visited {text-decoration:none; color:#eee;}
a.nav:hover {text-decoration:underline; color:#eee;}
a.nav:active {text-decoration:none; color:#eee;}
tr:nth-child(even) {background:#eee;}
tr:nth-child(odd) {background:#fff;}
tr.head {background:#eee;}
td {font-size: 1em; line-height: 1.5em;}
th {font-size: 1em; line-height: 1.5em;}
pre {
    margin:0px;
    padding-top:10px;
    padding-bottom:10px; 
    font-family:Monaco,Menlo,Consolas,"Courier New",monospace; 
    font-size:1em; 
    background-color:#eee; 
    color:#111;
    white-space: pre-wrap;       /* CSS 3 */
    white-space: -moz-pre-wrap;  /* Mozilla, since 1999 */
    white-space: -pre-wrap;      /* Opera 4-6 */
    white-space: -o-pre-wrap;    /* Opera 7 */
    word-wrap: break-word; 
}
</style>
</head>
<body>
<div  class="nav">&nbsp;&nbsp;&nbsp;<a href="../index.html" class="nav">..</a> DaRIS v@VERSION@</div>
<br/>
<ul>
<li><b>Version(git tag): v@VERSION@</b></li>
<li><b>Build-Time: @BUILD_TIME@</b></li>
</ul>
</br>
<table width="100%">
<thead>
<tr class="head">
    <th width="20%">name</th>
    <th width="10%">version</th>
    <th width="30%">download</th>
    <th width="20%">type</th>
    <th width="20%">require mediaflux version</th>
</tr>
</thead>
<tbody>
<tr>
    <td>daris-essentials</td>
    <td align="center">@ESSENTIALS_VERSION@</td>
    <td><a href="@ESSENTIALS_FILE@">@ESSENTIALS_FILE@</a></td>
    <td align="center">mediaflux plugin package</td>
    <td align="center">@MFLUX_VERSION@</td>
</tr>

<tr>
    <td>daris-core-services</td>
    <td align="center">@CORE_SERVICES_VERSION@</td>
    <td><a href="@CORE_SERVICES_FILE@">@CORE_SERVICES_FILE@</a></td>
    <td align="center">mediaflux plugin package</td>
    <td align="center">@MFLUX_VERSION@</td>
</tr>

<tr>
    <td>daris-portal</td>
    <td align="center">@PORTAL_VERSION@</td>
    <td><a href="@PORTAL_FILE@">@PORTAL_FILE@</a></td>
    <td align="center">mediaflux plugin package</td>
    <td align="center">@MFLUX_VERSION@</td>
</tr>

<tr>
    <td>daris-transcoders</td>
    <td align="center">@TRANSCODERS_VERSION@</td>
    <td><a href="@TRANSCODERS_FILE@">@TRANSCODERS_FILE@</a></td>
    <td align="center">mediaflux plugin package</td>
    <td align="center">@MFLUX_VERSION@</td>
</tr>

<tr>
    <td>daris-sinks</td>
    <td align="center">@SINKS_VERSION@</td>
    <td><a href="@SINKS_FILE@">@SINKS_FILE@</a></td>
    <td align="center">mediaflux plugin package</td>
    <td align="center">@MFLUX_VERSION@</td>
</tr>

<tr>
    <td>daris-analyzers</td>
    <td align="center">@ANALYZERS_VERSION@</td>
    <td><a href="@ANALYZERS_FILE@">@ANALYZERS_FILE@</a></td>
    <td align="center">mediaflux plugin package</td>
    <td align="center">@MFLUX_VERSION@</td>
</tr>

<tr>
    <td>daris-client-pvupload</td>
    <td align="center">@PVUPLOAD_VERSION@</td>
    <td><a href="@PVUPLOAD_FILE@">@PVUPLOAD_FILE@</a></td>
    <td align="center">mediaflux client application</td>
    <td align="center">N/A</td>
</tr>

<tr>
    <td>daris-client-server-config</td>
    <td align="center">@SERVER_CONFIG_VERSION@</td>
    <td><a href="@SERVER_CONFIG_FILE@">@SERVER_CONFIG_FILE@</a></td>
    <td align="center">mediaflux client application</td>
    <td align="center">N/A</td>
</tr>

<tr>
    <td>daris-client-dicom-client</td>
    <td align="center">@DICOM_CLIENT_VERSION@</td>
    <td><a href="@DICOM_CLIENT_FILE@">@DICOM_CLIENT_FILE@</a></td>
    <td align="center">mediaflux client application</td>
    <td align="center">N/A</td>
</tr>

<tr>
    <td>daris-client-mbcpetct-download</td>
    <td align="center">@MBCPETCT_DOWNLOAD_VERSION@</td>
    <td><a href="@MBCPETCT_DOWNLOAD_FILE@">@MBCPETCT_DOWNLOAD_FILE@</a></td>
    <td align="center">mediaflux client application</td>
    <td align="center">N/A</td>
</tr>

<tr>
    <td>daris-client-mbcpetct-upload</td>
    <td align="center">@MBCPETCT_UPLOAD_VERSION@</td>
    <td><a href="@MBCPETCT_UPLOAD_FILE@">@MBCPETCT_UPLOAD_FILE@</a></td>
    <td align="center">mediaflux client application</td>
    <td align="center">N/A</td>
</tr>

<tr>
    <td>daris-commons</td>
    <td align="center">@COMMONS_VERSION@</td>
    <td><a href="@COMMONS_FILE@">@COMMONS_FILE@</a></td>
    <td align="center">library</td>
    <td align="center">N/A</td>
</tr>

<tr>
    <td>daris-dcmtools</td>
    <td align="center">@DCMTOOLS_VERSION@</td>
    <td><a href="@DCMTOOLS_FILE@">@DCMTOOLS_FILE@</a></td>
    <td align="center">library & CLI applications</td>
    <td align="center">N/A</td>
</tr>

</tbody>
</table>
<br/>
<h3>Change Log [@BUILD_TIME@]</h3>
<pre>
</pre>
</body>
</html>