import json
import urllib3
from flask import Flask
from flask import request, redirect, url_for, render_template

ip = '127.0.0.1'     #IP OF THE VERTX SERVER
port = '8083'       #PORT OF THE VERTX SERVER
http = urllib3.PoolManager(num_pools = 4) #INITIALIZE THE SOCKET OF CONNECTION
rh2 = http.request('GET',str(ip)+':'+str(port)+'/api/humedad/getlasts', retries = 5, timeout = 4.0) #TEST REQUEST TO EVALUATE IF THE CONECTION IS WORKING, IF NOT AN ERROR IS DISPLAYED AND THE PROGRAM IS STOPPED
project_infoh = json.loads(rh2.data) #DECODE THE JSON OBTAINED TO SEE IF THE DATA IS CORRECT (DEBUG PURPUSO)
app = Flask(__name__) #INITIALIZE THE WEB SERVER
head = '<head><img src = "/static/logo.png" alt = "yupy logo"><br></head>' #HEAD OF THE HTML PAGES WITH THE LOGO
style = '<link rel ="stylesheet" type = "text/css" href = /static/main.css" />' #STYLE OF THE PAGES

@app.route("/")
def main():
    #INITIAL PAGE
    #return  render_template('/template/index.html')  
    return  '<html>'+style+head+'<body><a id="mybutton" href="/Lectura" title="/Lectura"><button>Lectura</button></a> <br><a id="Regar" href="/Regar" title="/Regar"> <button>Sensores</button></a> <br><a id="Sensores" href="/Sensores" title="/Sensores"> <button>Sensores</button></a> </body></html> ' #'<span>humedad actual del sensor: ' + str(project_infoh[0]['value']) +'</span>' + 'hola'
@app.route("/Lectura",methods=['GET'])
def Lectura():
    #SHOW THE VALUES OF THE SENSORS SORTING AND GROUPING THEM BY DEVICE AND SHOWING USING GOOGLE CHARTS

    rh2 = http.request('GET',str(ip)+':'+str(port)+'/api/humedad/getlasts', retries = 5, timeout = 4.0)
    rt2 = http.request('GET',str(ip)+':'+str(port)+'/api/temperatura/getlasts', retries = 5, timeout = 4.0)
    rl2 = http.request('GET',str(ip)+':'+str(port)+'/api/luz/getlasts', retries = 5, timeout = 4.0)
    project_infoh = json.loads(rh2.data)
    project_infol = json.loads(rl2.data)
    project_infot = json.loads(rt2.data)
     
    ret = ' <html>   <head> <img src = "/static/logo.png" alt = "yupy logo"><br>    <script type="text/javascript" src="https://www.google.com/jsapi"></script></head>'
    for i in range(0,len(project_infoh),1):
        if project_infoh[i]['value'] >= 500:
            ColorH = "blue"
        else :
            ColorH = "brown"
        if project_infot[i]['value'] >= 500:
            ColorL = "yellow"
        else :
            ColorL = "black"
        if project_infol[i]['value'] >= 500:
            ColorT = "red"
        else :
            ColorT = "blue"
        ret = ret + '''  
    <div id="chart_div'''+str(i)+'''"  style="width: 900px; height: 300px;"></div>
    <script type="text/javascript">
    google.load("visualization", "1", {packages: ["corechart", "bar"]});
    google.setOnLoadCallback(drawBasic);
    function drawBasic() {
      var data'''+str(i)+''' = google.visualization.arrayToDataTable([
        ["Sensor of ID ''' + str(id) + ''' ", "Percentage", { role: "style" }],
        ["HUMIDITY",'''+str((project_infoh[i]['value']-24)/10)+ ''', "'''+ColorH+'''"],
        ["LIGHT", '''+str((project_infol[i]['value']-24)/10)+ ''', "'''+ColorL+'''"],
        ["TEMPERATURE", '''+str((project_infot[i]['value']-24)/10)+ ''', "'''+ColorL+'''"]
      ]);
      var options = {
        title: "Sensors of ID '''+str(i+1)+'''",
        chartArea: {width: "50%"},
        hAxis: {
          title: "",
          minValue: 0,
          maxValue: 100
        },
        vAxis: {
          title: ""
  }
      };
    var chart'''+str(i)+''' = new google.visualization.BarChart(document.getElementById("chart_div'''+str(i)+'''")); 
      chart'''+str(i)+'''.draw(data'''+str(i)+''', options);
    }
    </script>  
    '''
        ret = ret +'<form action = "Regar0" value ='+str(i)+' method = "post"><input type="submit" name="riego" value="0'+str((i+1))+'" /></form><form action = "Regar1" value ='+str(i)+' method = "post"><input type="submit" name="riego" value="0'+str((i+1))+'" /></form>'
    ret = ret + '<body>     <!--Div that will hold the pie chart-->     <div id="chart_div"></div>   </body> </html>'
    return ret
@app.route("/Sensores", methods = ['GET','PUT'])
#SHOW THE INFORMATION OF ALL THE SENSORS(OLD VERSION OF THE LECTURA PAGE)
def sensores():
    ret = ''
    rh2 = http.request('GET',str(ip)+':'+str(port)+'/api/humedad/getlasts', retries = 5, timeout = 4.0)
    rt2 = http.request('GET',str(ip)+':'+str(port)+'/api/temperatura/getlasts', retries = 5, timeout = 4.0)
    rl2 = http.request('GET',str(ip)+':'+str(port)+'/api/luz/getlasts', retries = 5, timeout = 4.0)
    project_infoh = json.loads(rh2.data)
    project_infol = json.loads(rl2.data)
    project_infot = json.loads(rt2.data)

    for i in range(0,len(project_infoh),1):
        ret = ret +'<br>Sensor de humedad '+str(project_infoh[i]['id_SensorH'])+'<canvas id="myCanvas'+str(i)+' " width="254" height="30" style="border:1px solid #d3d3d3;">Your browser does not support the HTML5 canvas tag.</canvas><script>var c'+str(i)+' = document.getElementById("myCanvas'+str(i)+' ");var ctx'+str(i)+' = c'+str(i)+'.getContext("2d");var grd'+str(i)+' = ctx'+str(i)+'.createLinearGradient(0,0,200,0);grd'+str(i)+'.addColorStop(0,"blue");grd'+str(i)+'.addColorStop(1,"white");ctx'+str(i)+'.fillStyle = grd'+str(i)+';ctx'+str(i)+'.fillRect(10,10,' + str(project_infoh[i]['value']) +',10);</script>' +'<form action = "Regar0" value ='+str(i)+' method = "post"><input type="submit" name="riego" value="0'+str(i+1)+'" /></form><form action = "Regar1" value ='+str(i)+' method = "post"><input type="submit" name="riego" value="0'+str(i+1)+'" /></form>'#ret = ret +'<br>' + '<span>humedad actual del sensor '+str(i)+': ' + str(project_infoh[0]['value']) +'</span> '+ '</br>'
          
    for i in range(0,len(project_infol),1):
        ret =  ret +'<br>Sensor de Luz '+str(project_infol[i]['id_SensorL'])+'<canvas id="myCanvas'+str(i)+' " width="254" height="30" style="border:1px solid #d3d3d3;">Your browser does not support the HTML5 canvas tag.</canvas><script>var c'+str(i)+' = document.getElementById("myCanvas'+str(i)+' ");var ctx'+str(i)+' = c'+str(i)+'.getContext("2d");var grd'+str(i)+' = ctx'+str(i)+'.createLinearGradient(0,0,200,0);grd'+str(i)+'.addColorStop(0,"blue");grd'+str(i)+'.addColorStop(1,"white");ctx'+str(i)+'.fillStyle = grd'+str(i)+';ctx'+str(i)+'.fillRect(10,10,' + str(project_infol[i]['value']) +',10);</script>' 
    for i in range(0,len(project_infot),1):
        ret =  ret +'<br>Sensor de temperatura '+str(project_infot[i]['id_SensorT'])+'<canvas id="myCanvas'+str(i)+' " width="254" height="30" style="border:1px solid #d3d3d3;">Your browser does not support the HTML5 canvas tag.</canvas><script>var c'+str(i)+' = document.getElementById("myCanvas'+str(i)+' ");var ctx'+str(i)+' = c'+str(i)+'.getContext("2d");var grd'+str(i)+' = ctx'+str(i)+'.createLinearGradient(0,0,200,0);grd'+str(i)+'.addColorStop(0,"blue");grd'+str(i)+'.addColorStop(1,"white");ctx'+str(i)+'.fillStyle = grd'+str(i)+';ctx'+str(i)+'.fillRect(10,10,' + str(project_infot[i]['value']) +',10);</script>' 

        ret = ret + '</body></html>'
    return ret
@app.route('/Regar0', methods = ['GET','POST'])
#DEACTIVATE THE ACTUATOR OF THE DESIRED DEVICE
def regar0():
  #  r = http.request('GET','127.0.0.1:8084/api/riego', retries = 5, timeout = 4.0)
  #  rloaded = json.loads(r.data)
    if request.method == 'POST':
        data = {'id':request.form['riego'], 'Action': '0'}
        jsondata =  json.dumps(data).encode('utf-8')
        req = http.request('PUT', str(ip)+':'+str(port)+'/api/MQTT', body = jsondata)
        return str(req.data) + '<html><script type="text/javascript"> setTimeout(function(){history.back();}, 3000);</script> </html>'
        #if req.data == 200:
         #   return redirect('/Regar')
        #else: 
       #     return redirect('/')
    else:
        button = ''
        for i in range(0,5,1):
            button = button + '<br>' + '<button class><form action = "Regar" value ='+str(i)+' method = "post"><input type="submit" name="riego" value="0'+str(i+1)+'" /></form></button>' +' </br>'
            button = button +'.button {background-color: #4CAF50; /* Green */ border: none; color: white; padding: 15px 32px;  text-align: center;  text-decoration: none;  display: inline-block;  font-size: 16px;}'
        return str(button)
@app.route('/Regar1', methods = ['GET','POST'])
#ACTIVATE THE ACTUATOR OF THE DESIRED DEVICE
def regar1():
  #  r = http.request('GET','127.0.0.1:8084/api/riego', retries = 5, timeout = 4.0)
  #  rloaded = json.loads(r.data)
    if request.method == 'POST':
        data = {'id':request.form['riego'], 'Action': '1'}
        jsondata =  json.dumps(data).encode('utf-8')
        req = http.request('PUT', str(ip)+':'+str(port)+'/api/MQTT', body = jsondata)
        return str(req.data) + '<html><script type="text/javascript"> setTimeout(function(){history.back();}, 3000);</script> </html>'
        #if req.data == 200:
         #   return redirect('/Regar')
        #else: 
       #     return redirect('/')
    else:
        button = ''
        for i in range(0,5,1):
            button = button + '<br>' + '<button class><form action = "Regar" value ='+str(i)+' method = "post"><input type="submit" name="riego" value="0'+str(i+1)+'" /></form></button>' +' </br>'
            button = button +'.button {background-color: #4CAF50; /* Green */ border: none; color: white; padding: 15px 32px;  text-align: center;  text-decoration: none;  display: inline-block;  font-size: 16px;}'
        return str(button)

#DEVELOP AND DEBUG OF NEW FUNCTIONALITIES
@app.route('/debug', methods = ['GET','POST'])
def debug():
    rh2 = http.request('GET',str(ip)+':'+str(port)+'/api/humedad/getlasts', retries = 5, timeout = 4.0)
    rt2 = http.request('GET',str(ip)+':'+str(port)+'/api/temperatura/getlasts', retries = 5, timeout = 4.0)
    rl2 = http.request('GET',str(ip)+':'+str(port)+'/api/luz/getlasts', retries = 5, timeout = 4.0)
    project_infoh = json.loads(rh2.data)
    project_infol = json.loads(rl2.data)
    project_infot = json.loads(rt2.data)
    if project_infoh[0]['value'] >= 500:
        ColorH = "blue"
    else :
        ColorH = "brown"
    if project_infot[0]['value'] >= 500:
        ColorL = "yellow"
    else :
        ColorL = "black"
    if project_infol[0]['value'] >= 500:
        ColorT = "red"
    else :
        ColorT = "blue"
    ret = ' <html>   <head>     <script type="text/javascript" src="https://www.google.com/jsapi"></script></head>'
    for i in range(0,len(project_infoh),1):
        ret = ret + '''  
    <div id="chart_div'''+str(i)+'''"  style="width: 900px; height: 300px;"></div>
    <script type="text/javascript">
    google.load("visualization", "1", {packages: ["corechart", "bar"]});
    google.setOnLoadCallback(drawBasic);
    function drawBasic() {
      var data'''+str(i)+''' = google.visualization.arrayToDataTable([
        ["Sensor of ID ''' + str(id) + ''' ", "Density", { role: "style" }],
        ["HUMIDITY",'''+str(project_infoh[i]['value'])+ ''', "'''+ColorH+'''"],
        ["LIGHT", '''+str(project_infol[i]['value'])+ ''', "'''+ColorL+'''"],
        ["TEMPERATURE", '''+str(project_infot[i]['value'])+ ''', "'''+ColorL+'''"]
      ]);
      var options = {
        title: "Sensors of ID x",
        chartArea: {width: "50%"},
        hAxis: {
          title: "",
          minValue: 0,
          maxValue: 1024
        },
        vAxis: {
          title: ""
  }
      };
    var chart'''+str(i)+''' = new google.visualization.BarChart(document.getElementById("chart_div'''+str(i)+'''")); 
      chart'''+str(i)+'''.draw(data'''+str(i)+''', options);
    }
    </script>  
    '''
    ret = ret + '<body>     <!--Div that will hold the pie chart-->     <div id="chart_div"></div>   </body> </html>'
    return ret
if __name__ == "__main__":
    app.run(host="0.0.0.0")  #TELL THE FLASK SERVER TO LOOK FOR CONNECTIONS COMING FROM ALL THE IPS, NOT ONLY THE LOCAL HOST ONES

