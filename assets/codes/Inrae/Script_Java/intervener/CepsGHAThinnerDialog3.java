/*
 * The Forceps model.
 * 
 * Copyright (C) April 2013: X. Morin (CNRS CEFE).
 * 
 * This file is part of the Forceps model and is NOT free software. It is the property of its
 * authors and must not be copied without their permission. It can be shared by the modellers of the
 * Capsis co-development community in agreement with the Capsis charter
 * (http://capsis.cirad.fr/capsis/charter). See the license.txt file in the Capsis installation
 * directory for further information about licenses in Capsis.
 */

package forceps.extension.intervener;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jeeb.lib.util.AmapDialog;
import jeeb.lib.util.ColumnPanel;
import jeeb.lib.util.LinePanel;
import jeeb.lib.util.MessageDialog;
import jeeb.lib.util.Settings;
import jeeb.lib.util.Translator;
import capsis.commongui.util.Helper;

/**
 * This dialog box is used to config CepsGHAThinner3.
 * 
 * @author X. Morin, Lisa Grell, F. de Coligny - March 2021, 
 */
public class CepsGHAThinnerDialog3 extends AmapDialog implements ActionListener {

	private NumberFormat nf;
	private CepsGHAThinner3 thinner;

	// Basal area of the scene (m2/ha)
	private double sceneG;

	private JTextField minCutG;
	private JTextField type;
	private JTextField finalG;
//	private JTextField expectedMixComposition;

	protected JButton ok;
	protected JButton cancel;
	protected JButton help;

	/**
	 * Constructor
	 */
	public CepsGHAThinnerDialog3(CepsGHAThinner3 thinner) {
		super();

		this.thinner = thinner;

		sceneG = thinner.getGha(thinner.scene.getTrees());

		createUI();

		// fc-31.8.2018
		setTitle (thinner.getName());
//		setTitle(Translator.swap("CepsGHAThinner3"));

		setModal(true);

		activateSizeMemorization(getClass ().getName ());
		
		// location is set by the AmapDialog superclass
		pack(); // uses component's preferredSize
		show();

	}

	/**
	 * Action on ok button.
	 */
	private void okAction() {

		try {
			thinner.setMinCutG(Double.parseDouble(minCutG.getText().trim()));
		} catch (Exception e) {
			MessageDialog.print(this, Translator.swap("CepsGHAThinnerDialog3.wrongMinCutG"), e);
			return;
		}

		try {
			thinner.setType(Double.parseDouble(type.getText().trim()));
		} catch (Exception e) {
			MessageDialog.print(this, Translator.swap("CepsGHAThinnerDialog3.wrongType"), e);
			return;
		}

		try {
			thinner.setFinalG(finalG.getText().trim());
		} catch (Exception e) {
			MessageDialog.print(this, Translator.swap("CepsGHAThinnerDialog3.wrongFinalG"), e);
			return;
		}

//		try {
//			thinner.setExpectedMixComposition(expectedMixComposition.getText().trim());
//		} catch (Exception e) {
//			MessageDialog.print(this, Translator.swap("CepsGHAThinnerDialog3.wrongExpectedMixComposition"), e);
//			return;
//		}

		Settings.setProperty ("CepsGHAThinnerDialog3.minCutG", minCutG.getText ().trim ());
		Settings.setProperty ("CepsGHAThinnerDialog3.type", type.getText ().trim ());
		Settings.setProperty ("CepsGHAThinnerDialog3.finalG", finalG.getText ().trim ());
//		Settings.setProperty ("CepsGHAThinnerDialog3.expectedMixComposition", expectedMixComposition.getText ().trim ());
		
		// All has been checked successfully, set the dialog invisible
		// and go back to caller (will check for validity and dispose the
		// dialog)
		setValidDialog(true);
	}

	/**
	 * Action on cancel button.
	 */
	private void cancelAction() {
		// Set the dialog invisible
		// and go back to caller (will check for validity and dispose the
		// dialog)
		setValidDialog(false);
	}

	/**
	 * Someone hit a button.
	 */
	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource().equals(ok)) {
			okAction();
		} else if (evt.getSource().equals(cancel)) {
			cancelAction();
		} else if (evt.getSource().equals(help)) {
			Helper.helpFor(this);
		}
	}

	/**
	 * Creates the dialog box user interface.
	 */
	private void createUI() {

		// vertical panel for sliders
		ColumnPanel c1 = new ColumnPanel();

		// Scene G for information
		LinePanel l0 = new LinePanel();
		l0.add(new JLabel(Translator.swap("CepsGHAThinnerDialog3.sceneG") + " : "));
		JTextField sceneG = new JTextField();
		sceneG.setText("" + this.sceneG);
		// sceneGHA.setText(""+this.sceneGHA);
		sceneG.setEditable(false);
		l0.add(sceneG);
		l0.addStrut0();
		c1.add(l0);

		LinePanel l1 = new LinePanel();
		l1.add(new JLabel(Translator.swap("CepsGHAThinnerDialog3.minCutG") + " : "));
		minCutG = new JTextField();
		minCutG.setText (""+Settings.getProperty ("CepsGHAThinnerDialog3.minCutG", 0));
		l1.add(minCutG);
		l1.addStrut0();
		c1.add(l1);

		LinePanel l2 = new LinePanel();
		l2.add(new JLabel(Translator.swap("CepsGHAThinnerDialog3.type") + " : "));
		type = new JTextField();
		type.setText (""+Settings.getProperty ("CepsGHAThinnerDialog3.type", 0));
		l2.add(type);
		l2.addStrut0();
		c1.add(l2);

		LinePanel l3 = new LinePanel();
		l3.add(new JLabel(Translator.swap("CepsGHAThinnerDialog3.finalG") + " : "));
		finalG = new JTextField();
		finalG.setText (""+Settings.getProperty ("CepsGHAThinnerDialog3.finalG", "0"));
		l3.add(finalG);
		l3.addStrut0();
		c1.add(l3);

//		LinePanel l4 = new LinePanel();
//		l4.add(new JLabel(Translator.swap("CepsGHAThinnerDialog3.expectedMixComposition") + " : "));
//		expectedMixComposition = new JTextField();
//		expectedMixComposition.setText (""+Settings.getProperty ("CepsGHAThinnerDialog3.expectedMixComposition", ""));
//		l4.add(expectedMixComposition);
//		l4.addStrut0();
//		c1.add(l4);

		c1.addGlue();

		// Control Line : Ok Cancel Help
		LinePanel controlPanel = new LinePanel();
		ok = new JButton(Translator.swap("Shared.ok"));
		cancel = new JButton(Translator.swap("Shared.cancel"));
		help = new JButton(Translator.swap("Shared.help"));

		controlPanel.addGlue(); // adding glue first -> the buttons will be
								// right justified
		controlPanel.add(ok);
		controlPanel.add(cancel);
		controlPanel.add(help);
		controlPanel.addStrut0();

		ok.addActionListener(this); // listeners
		cancel.addActionListener(this);
		help.addActionListener(this);

		// Put the label panels to the left, the text boxes to the right
		// and control to the bottom
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(c1, BorderLayout.NORTH);
		getContentPane().add(controlPanel, BorderLayout.SOUTH);

		// Set Ok as default (see AmapDialog)
		setDefaultButton(ok);

	}

}
