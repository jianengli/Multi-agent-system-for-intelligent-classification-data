/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package assignment1;

import java.awt.BorderLayout;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author The whole project is coded by both of us, Jianeng Li and Junwei Gong
 */
public class ShowKmeansImage extends javax.swing.JFrame {

   
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
public ShowKmeansImage() {
		JPanel panel=new JPanel(new BorderLayout());
		JPanel panel2=new JPanel(new BorderLayout());
		JPanel panel3=new JPanel(new BorderLayout());
		
		String urlString="F:/R project/plot.png";
		JLabel label=new JLabel(new ImageIcon(urlString));
		
		panel.add(label,BorderLayout.CENTER);
		
		
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(panel,BorderLayout.CENTER);
		this.getContentPane().add(panel2,BorderLayout.SOUTH);
		this.getContentPane().add(panel3,BorderLayout.EAST);
		
		this.setSize(1200, 800);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("the Result of K-means: between fixedacidity and volatileacidity");
		this.setVisible(true);
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
