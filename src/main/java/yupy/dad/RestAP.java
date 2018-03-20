package yupy.dad;





import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.asyncsql.MySQLClient;

public class RestAP extends AbstractVerticle {

	private SQLClient mySQLClient;

	public void start(Future<Void> startFuture) {

		JsonObject mySQLClientConfig = new JsonObject().put("host", "127.0.0.1").put("port", 3306)
				.put("database", "yupy").put("username", "root").put("password", "root");

		mySQLClient = MySQLClient.createShared(vertx, mySQLClientConfig);
		Router router = Router.router(vertx);
		vertx.createHttpServer().requestHandler(router::accept).listen(8083, res -> {
			if (res.succeeded()) {
				System.out.println("Servidor desplegado");

			} else {
				System.out.println("Error:" + res.cause());
			}

		});
		
		// HUMEDAD
		
		router.route("/api/humedad").handler(BodyHandler.create());
		router.get("/api/humedad").handler(this::getAll_H);
		router.put("/api/humedad/put/:id_SensorH,:id,:state,:fecha,:value").handler(this::putOne_H);
		router.get("/api/humedad/:id").handler(this::getOne_H);
		router.get("/api/humedad/delete/delete").handler(this::deleteAll_H);
		
		//TEMPERTURA
		router.route("/api/temperatura").handler(BodyHandler.create());
		router.get("/api/temperatura").handler(this::getAll_T);
		router.put("/api/temperatura/put/:id_SensorT,:id,:state,:fecha,:value").handler(this::putOne_T);
		router.get("/api/temperatura/:id").handler(this::getOne_T);
		router.get("/api/temperatura/delete/delete").handler(this::deleteAll_T);
		//ACIDEZ
		router.route("/api/acidez").handler(BodyHandler.create());
		router.get("/api/acidez").handler(this::getAll_A);
		router.put("/api/acidez/put/:id_SensorA,:id,:state,:fecha,:value").handler(this::putOne_A);
		router.get("/api/acidez/:id").handler(this::getOne_A);
		router.get("/api/acidez/delete/delete").handler(this::deleteAll_A);
		//LUZ
		router.route("/api/luz").handler(BodyHandler.create());
		router.get("/api/luz").handler(this::getAll_L);
		router.put("/api/luz/put/:id_SensorL,:id,:state,:fecha,:value").handler(this::putOne_L);
		router.get("/api/luz/:id").handler(this::getOne_L);
		router.get("/api/luz/delete/delete").handler(this::deleteAll_L);
		
		
		

	}

	// METODOS HUMEDAD
	
	private void getAll_H(RoutingContext routingContext) {
	
		try {
			
			
			mySQLClient.getConnection(conn->{
				if(conn.succeeded()){
					SQLConnection connection=conn.result();
					String query="SELECT id_SensorH , id , fecha , state , value"+" FROM humedad";
				
					connection.query(query,
							res->{
						if(res.succeeded()){
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							
						}else{
							routingContext.response().setStatusCode(400).end("Error:"+res.cause());
						}
					}
							);
				}else{
					routingContext.response().setStatusCode(400).end("Error:"+conn.cause());
				}
				
				
			});
			//routingContext.response().setStatusCode(200).
			//end(Json.encodePrettily(database.get(param)));
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}


	}


	private void getOne_H(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");
		
		if (paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);
				

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT  id_SensorH , id , fecha , state , value" + " FROM humedad " + " WHERE id = ?";
						JsonArray paramQuery = new JsonArray().add(param);
						
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				// routingContext.response().setStatusCode(200).
				// end(Json.encodePrettily(database.get(param)));
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}
	private void deleteAll_H(RoutingContext routingContext) {
        try {


            mySQLClient.getConnection(conn -> {
                if(conn.succeeded()) {

                    SQLConnection connection = conn.result();
                    String query = "DELETE FROM humedad" ; // la ?  sera igual a param



                    connection.query(query,  res -> {

                        if(res.succeeded()) {
                            routingContext.response().end(Json.encode("Succefully deleted"));

                            res.result().getRows();

                        }else {
                            routingContext.response().setStatusCode(400).end(
                                    res.cause().toString()); //"Error: " + res.cause();

                        }

                    });

                }else {
                    routingContext.response().setStatusCode(400).end(
                            conn.cause().toString()); //"Error: " + conn.cause();
                }

            });

            //routingContext.response().setStatusCode(200)
        //    .end(Json.encodePrettily(database.get(param)));

        } catch (ClassCastException e) {
            routingContext.response().setStatusCode(400).end();


        }


  }
	private void putOne_H(RoutingContext routingContext) {

		try {
		
			
			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					
					Humedad_State state = Json.decodeValue(routingContext.getBodyAsString(), Humedad_State.class);
					
				
					String query = "INSERT INTO humedad VALUES(?,?,?,?,?)";
					JsonArray paramQuery = new JsonArray().add(state.getId_SensorH());
					paramQuery.add(state.getId());
					paramQuery.add(state.isState());
					paramQuery.add(state.getFecha());
					paramQuery.add(state.getValue());
					connection.updateWithParams(query,paramQuery, res -> {
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily("Succefully added"));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}
					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
			// routingContext.response().setStatusCode(200).
			// end(Json.encodePrettily(database.get(param)));
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}
	
	}
	
	
	//METODOS TEMPERATURA
	
	private void getAll_T(RoutingContext routingContext) {
		
		try {
			
			
			mySQLClient.getConnection(conn->{
				if(conn.succeeded()){
					SQLConnection connection=conn.result();
					String query="SELECT id_SensorT , id , state , fecha , value"+" FROM temperatura";
				
					connection.query(query,
							res->{
						if(res.succeeded()){
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							
						}else{
							routingContext.response().setStatusCode(400).end("Error:"+res.cause());
						}
					}
							);
				}else{
					routingContext.response().setStatusCode(400).end("Error:"+conn.cause());
				}
				
				
			});
			//routingContext.response().setStatusCode(200).
			//end(Json.encodePrettily(database.get(param)));
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}


	}
	
	private void getOne_T(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");
		
		if (paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);
				

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT  id_SensorT , id , state , fecha , value" + " FROM temperatura " + " WHERE id = ?";
						JsonArray paramQuery = new JsonArray().add(param);
						
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				// routingContext.response().setStatusCode(200).//prueba con map
				// end(Json.encodePrettily(database.get(param)));
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void deleteAll_T(RoutingContext routingContext) {
        try {


            mySQLClient.getConnection(conn -> {
                if(conn.succeeded()) {

                    SQLConnection connection = conn.result();
                    String query = "DELETE FROM yupy.temperatura" ; // la ?  sera igual a param



                    connection.query(query,  res -> {

                        if(res.succeeded()) {
                            routingContext.response().end(Json.encode("Succefully deleted"));

                           

                        }else {
                            routingContext.response().setStatusCode(400).end(
                                    res.cause().toString()); //"Error: " + res.cause();

                        }

                    });

                }else {
                    routingContext.response().setStatusCode(400).end(
                            conn.cause().toString()); //"Error: " + conn.cause();
                }

            });

            //routingContext.response().setStatusCode(200)
        //    .end(Json.encodePrettily(database.get(param)));

        } catch (ClassCastException e) {
            routingContext.response().setStatusCode(400).end();


        }
	
	}
	
	private void putOne_T(RoutingContext routingContext) {


		
			try {
			
				
				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						
						Temperatura_State state = Json.decodeValue(routingContext.getBodyAsString(), Temperatura_State.class);
						
					
						String query = "INSERT INTO temperatura VALUES(?,?,?,?,?)";
						JsonArray paramQuery = new JsonArray().add(state.getId_SensorT());
						paramQuery.add(state.getId());
						paramQuery.add(state.isState());
						paramQuery.add(state.getFecha());
						paramQuery.add(state.getValue());
						connection.updateWithParams(query,paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily("Succefully added"));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				// routingContext.response().setStatusCode(200).
				// end(Json.encodePrettily(database.get(param)));
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		

	}
//METODOS ACIDEZ
private void getAll_A(RoutingContext routingContext) {
		
		try {
			
			
			mySQLClient.getConnection(conn->{
				if(conn.succeeded()){
					SQLConnection connection=conn.result();
					String query="SELECT id_SensorA , id , state , fecha , value"+" FROM acidez";
				
					connection.query(query,
							res->{
						if(res.succeeded()){
							routingContext.response().end(Json.encodePrettily(res.result().getRows()));
							
						}else{
							routingContext.response().setStatusCode(400).end("Error:"+res.cause());
						}
					}
							);
				}else{
					routingContext.response().setStatusCode(400).end("Error:"+conn.cause());
				}
				
				
			});
			//routingContext.response().setStatusCode(200).
			//end(Json.encodePrettily(database.get(param)));
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}


	}
	
	private void getOne_A(RoutingContext routingContext) {
		String paramStr = routingContext.request().getParam("id");
		
		if (paramStr != null) {
			try {
				int param = Integer.parseInt(paramStr);
				

				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						String query = "SELECT  id_SensorA , id , state , fecha , value" + " FROM acidez " + " WHERE id = ?";
						JsonArray paramQuery = new JsonArray().add(param);
						
						connection.queryWithParams(query, paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				// routingContext.response().setStatusCode(200).//prueba con map
				// end(Json.encodePrettily(database.get(param)));
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		} else {
			routingContext.response().setStatusCode(400).end();
		}

	}

	private void deleteAll_A(RoutingContext routingContext) {
        try {


            mySQLClient.getConnection(conn -> {
                if(conn.succeeded()) {

                    SQLConnection connection = conn.result();
                    String query = "DELETE FROM yupy.acidez" ; // la ?  sera igual a param



                    connection.query(query,  res -> {

                        if(res.succeeded()) {
                            routingContext.response().end(Json.encode("Succefully deleted"));

                            

                        }else {
                            routingContext.response().setStatusCode(400).end(
                                    res.cause().toString()); //"Error: " + res.cause();

                        }

                    });

                }else {
                    routingContext.response().setStatusCode(400).end(
                            conn.cause().toString()); //"Error: " + conn.cause();
                }

            });

            //routingContext.response().setStatusCode(200)
        //    .end(Json.encodePrettily(database.get(param)));

        } catch (ClassCastException e) {
            routingContext.response().setStatusCode(400).end();


        }
	
	}
	
	private void putOne_A(RoutingContext routingContext) {


		try {
		
			
			mySQLClient.getConnection(conn -> {
				if (conn.succeeded()) {
					SQLConnection connection = conn.result();
					
					Acidez_State state = Json.decodeValue(routingContext.getBodyAsString(), Acidez_State.class);
					
				
					String query = "INSERT INTO temperatura VALUES(?,?,?,?,?)";
					JsonArray paramQuery = new JsonArray().add(state.getId_SensorA());
					paramQuery.add(state.getId());
					paramQuery.add(state.isState());
					paramQuery.add(state.getFecha());
					paramQuery.add(state.getValue());
					connection.updateWithParams(query,paramQuery, res -> {
						if (res.succeeded()) {
							routingContext.response().end(Json.encodePrettily("Succefully added"));

						} else {
							routingContext.response().setStatusCode(400).end("Error:" + res.cause());
						}
					});
				} else {
					routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
				}

			});
			// routingContext.response().setStatusCode(200).
			// end(Json.encodePrettily(database.get(param)));
		} catch (ClassCastException e) {
			routingContext.response().setStatusCode(400).end();
		}
	}
	//METODOS LUZ
	
		private void getAll_L(RoutingContext routingContext) {
			
			try {
				
				
				mySQLClient.getConnection(conn->{
					if(conn.succeeded()){
						SQLConnection connection=conn.result();
						String query="SELECT id_SensorL , id , state , fecha , value"+" FROM luz";
					
						connection.query(query,
								res->{
							if(res.succeeded()){
								routingContext.response().end(Json.encodePrettily(res.result().getRows()));
								
							}else{
								routingContext.response().setStatusCode(400).end("Error:"+res.cause());
							}
						}
								);
					}else{
						routingContext.response().setStatusCode(400).end("Error:"+conn.cause());
					}
					
					
				});
				//routingContext.response().setStatusCode(200).
				//end(Json.encodePrettily(database.get(param)));
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}


		}
		
		private void getOne_L(RoutingContext routingContext) {
			String paramStr = routingContext.request().getParam("id");
			
			if (paramStr != null) {
				try {
					int param = Integer.parseInt(paramStr);
					

					mySQLClient.getConnection(conn -> {
						if (conn.succeeded()) {
							SQLConnection connection = conn.result();
							String query = "SELECT  id_SensorL , id , state , fecha , value" + " FROM luz " + " WHERE id = ?";
							JsonArray paramQuery = new JsonArray().add(param);
							
							connection.queryWithParams(query, paramQuery, res -> {
								if (res.succeeded()) {
									routingContext.response().end(Json.encodePrettily(res.result().getRows()));

								} else {
									routingContext.response().setStatusCode(400).end("Error:" + res.cause());
								}
							});
						} else {
							routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
						}

					});
					// routingContext.response().setStatusCode(200).//prueba con map
					// end(Json.encodePrettily(database.get(param)));
				} catch (ClassCastException e) {
					routingContext.response().setStatusCode(400).end();
				}
			} else {
				routingContext.response().setStatusCode(400).end();
			}

		}

		private void deleteAll_L(RoutingContext routingContext) {
	        try {


	            mySQLClient.getConnection(conn -> {
	                if(conn.succeeded()) {

	                    SQLConnection connection = conn.result();
	                    String query = "DELETE FROM yupy.luz" ; // la ?  sera igual a param



	                    connection.query(query,  res -> {

	                        if(res.succeeded()) {
	                            routingContext.response().end(Json.encode("Succefully deleted"));

	                          

	                        }else {
	                            routingContext.response().setStatusCode(400).end(
	                                    res.cause().toString()); //"Error: " + res.cause();

	                        }

	                    });

	                }else {
	                    routingContext.response().setStatusCode(400).end(
	                            conn.cause().toString()); //"Error: " + conn.cause();
	                }

	            });

	            //routingContext.response().setStatusCode(200)
	        //    .end(Json.encodePrettily(database.get(param)));

	        } catch (ClassCastException e) {
	            routingContext.response().setStatusCode(400).end();


	        }
		
		}
		
		private void putOne_L(RoutingContext routingContext) {


			try {
			
				
				mySQLClient.getConnection(conn -> {
					if (conn.succeeded()) {
						SQLConnection connection = conn.result();
						
						Luz_State state = Json.decodeValue(routingContext.getBodyAsString(), Luz_State.class);
						
					
						String query = "INSERT INTO luz VALUES(?,?,?,?,?)";
						JsonArray paramQuery = new JsonArray().add(state.getId_SensorL());
						paramQuery.add(state.getId());
						paramQuery.add(state.isState());
						paramQuery.add(state.getFecha());
						paramQuery.add(state.getValue());
						connection.updateWithParams(query,paramQuery, res -> {
							if (res.succeeded()) {
								routingContext.response().end(Json.encodePrettily("Succefully added"));

							} else {
								routingContext.response().setStatusCode(400).end("Error:" + res.cause());
							}
						});
					} else {
						routingContext.response().setStatusCode(400).end("Error:" + conn.cause());
					}

				});
				// routingContext.response().setStatusCode(200).
				// end(Json.encodePrettily(database.get(param)));
			} catch (ClassCastException e) {
				routingContext.response().setStatusCode(400).end();
			}
		}
}


