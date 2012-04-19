<%@ page session="false" contentType="text/html;charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://jsftutorials.net/htmLib" prefix="htm"%>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="x"%>
<%@ taglib uri="http://sourceforge.net/projects/jsf-comp/easysi" prefix="si"%>
<%@ taglib uri="http://richfaces.org/rich" prefix="rich"%>
<%@ taglib uri="https://ajax4jsf.dev.java.net/ajax" prefix="a4j"%>

<%-- ######################################## 

							Alle Aktuellen Schritte in der �?bersicht

	#########################################--%>
<a4j:keepAlive beanName="BatchForm" />
<html>
<f:view locale="#{SpracheForm.locale}">
	<%@include file="inc/head.jsp"%>
	<body>

		<htm:table cellspacing="5" cellpadding="0" styleClass="layoutTable" align="center">
			<%@include file="inc/tbl_Kopf.jsp"%>
			<htm:tr>
				<%@include file="inc/tbl_Navigation.jsp"%>
				<htm:td valign="top" styleClass="layoutInhalt">

					<%-- ++++++++++++++++     Inhalt      ++++++++++++++++ --%>
					<h:form id="mytaskform" rendered="#{((LoginForm.maximaleBerechtigung == 1) || (LoginForm.maximaleBerechtigung == 2))}">
						<%-- Breadcrumb --%>
						<h:panelGrid id="id0" width="100%" columns="1" styleClass="layoutInhaltKopf">
							<h:panelGroup id="id1">
								<h:commandLink id="id2" value="#{msgs.startseite}" action="newMain" />
								<f:verbatim> &#8250;&#8250; </f:verbatim>
								<h:outputText id="id3" value="#{msgs.batches}" />
							</h:panelGroup>
						</h:panelGrid>

						<htm:table border="0" align="center" width="100%" cellpadding="15">
							<htm:tr>
								<htm:td>

									<%-- Ueberschrift --%>
									<htm:h3>
										<h:outputText id="id4" value="#{msgs.batches}" />
									</htm:h3>

									<%-- globale Warn- und Fehlermeldungen --%>
									<h:messages id="id5" globalOnly="true" errorClass="text_red" infoClass="text_blue" showDetail="true" showSummary="true" tooltip="true" />

									<htm:table cellpadding="3" cellspacing="0" width="100%" styleClass="eingabeBoxen">
										<htm:tr>
											<htm:td colspan="2" styleClass="eingabeBoxen_row1">
												<h:outputText value="#{msgs.auswahl}" />
											</htm:td>
										</htm:tr>
										<htm:tr>
											<htm:td width="50%" styleClass="eingabeBoxen_row2" style="padding-left: 20px;vertical-align:top;">
											
												<htm:h4>
													<h:outputText value="#{msgs.batches}" />
												</htm:h4>
												
												<h:panelGroup style="margin-bottom:10px;display:block;" >
													<h:inputText value="#{BatchForm.batchfilter}" style="width:350px"/>
													<h:commandButton action="#{BatchForm.filterBatches}" title="#{msgs.filter}" value="#{msgs.filter}" />
												</h:panelGroup>
												
												<h:selectManyListbox value="#{BatchForm.selectedBatches}" style="width:90%;margin-bottom:10px;display:block;" size="20">
													<si:selectItems var="batch" value="#{BatchForm.currentBatches}" itemLabel="#{batch.batchLabel}" itemValue="#{batch.batchId}" />
												</h:selectManyListbox>
									
												<h:panelGrid columns="1" cellpadding="2px">
													<h:commandLink action="#{BatchForm.loadProcessData}">
														<h:graphicImage alt="reload" value="/newpages/images/buttons/reload_doc.gif" style="vertical-align:middle" />
														<h:outputText value="#{msgs.loadProcessesOfBatch}" />
													</h:commandLink>
										
													<h:commandLink action="#{BatchForm.downloadDocket}">
														<h:graphicImage alt="/newpages/images/buttons/laufzettel_wide.png" value="/newpages/images/buttons/laufzettel_wide.png"
															style="vertical-align:middle" />
														<h:outputText value="#{msgs.laufzettelDrucken}" />
													</h:commandLink>
										
													<h:commandLink action="#{BatchForm.editProperties}">
														<h:graphicImage  alt="edit" value="/newpages/images/buttons/edit.gif" style="vertical-align:middle"  />
														<h:outputText value="#{msgs.eigenschaftBearbeiten}"/>
													</h:commandLink>

													<h:commandLink action="#{BatchForm.deleteBatch}" style="margin-left:7px;">
														<h:graphicImage alt="deleteBatch" value="/newpages/images/buttons/waste1a_20px.gif" style="vertical-align:middle;margin-right:7px" />
														<h:outputText value="#{msgs.deleteBatch}" />
													</h:commandLink>
										
												</h:panelGrid>					
											</htm:td>
											
											<htm:td width="50%" styleClass="eingabeBoxen_row2" style="padding-left: 20px;vertical-align:top;">
												<htm:h4>
													<h:outputText value="#{msgs.prozesse}" />
												</htm:h4>
												
												<h:panelGroup style="margin-bottom:10px;display:block;" >
													<h:inputText value="#{BatchForm.processfilter}" style="width:350px" />
													<h:commandButton action="#{BatchForm.filterProcesses}" value="#{msgs.filter}" title="#{msgs.filter}" />
												</h:panelGroup>
												
												<h:selectManyListbox value="#{BatchForm.selectedProcesses}" converter="ProcessConverter"  style="width:90%;margin-bottom:10px;display:block;" size="20">
													<f:selectItems value="#{BatchForm.currentProcessesAsSelectItems}" />
												</h:selectManyListbox>
								
												<h:panelGrid columns="1" cellpadding="2px">
													<h:commandLink action="#{BatchForm.loadBatchData}">
														<h:graphicImage alt="reload" value="/newpages/images/buttons/reload_doc.gif" style="vertical-align:middle" />
														<h:outputText value="#{msgs.loadAssociatedBatchOfProcess}" />
													</h:commandLink>
									
													<h:commandLink action="#{BatchForm.addProcessesToBatch}">
														<h:graphicImage alt="add" value="/newpages/images/buttons/ok.gif" style="vertical-align:middle" />
														<h:outputText value="#{msgs.addToSelectedBatch}" />
													</h:commandLink>
									
													<h:commandLink action="#{BatchForm.removeProcessesFromBatch}">
														<h:graphicImage alt="remove" value="/newpages/images/buttons/cancel1.gif" style="vertical-align:middle" />
														<h:outputText value="#{msgs.removeFromAssociatedBatch}" />
													</h:commandLink>
													
													<h:commandLink action="#{BatchForm.createNewBatch}">
														<h:graphicImage alt="new" value="/newpages/images/buttons/star_blue.gif" style="vertical-align:middle" />
														<h:outputText value="#{msgs.createNewBatchFromSelectedProcesses}" />
													</h:commandLink>
												</h:panelGrid>
											</htm:td>
										</htm:tr>
								
									</htm:table>

								</htm:td>
							</htm:tr>
						</htm:table>
					</h:form>
					<%-- ++++++++++++++++    // Inhalt      ++++++++++++++++ --%>
				</htm:td>
			</htm:tr>
			<%@include file="inc/tbl_Fuss.jsp"%>
		</htm:table>

	</body>

</f:view>

</html>