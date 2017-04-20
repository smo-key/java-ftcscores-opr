package com.arthurpachachura.oprlib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

import com.arthurpachachura.oprlib.REST.Method;
import com.arthurpachachura.oprlib.Requests.Event;

public final class Runner {

	public static class TeamOPR {
		private double opr, ccwm;
		private Requests.Event.Ranking team;
		
		TeamOPR(Requests.Event.Ranking team, double opr, double ccwm) {
			this.team = team;
			this.opr = opr;
			this.ccwm = ccwm;
		}
		
		public int getTeamRank() {
			return team.rank;
		}
		public int getTeamNumber() {
			return team.number;
		}
		public String getTeamName() {
			return team.name;
		}
		public double getOPR() {
			return opr;
		}
		public double getCCWM() {
			return ccwm;
		}
		public double getDPR() {
			return opr - ccwm;
		}
		
		@Override
		public int hashCode() {
			return team.number;
		}		
		
		@Override
		public boolean equals(Object a) {
			return ((TeamOPR)a).getTeamNumber() == this.getTeamNumber();
		}
	}
	
	public static String ordinate(int i) {
	    int mod100 = i % 100;
	    int mod10 = i % 10;
	    if(mod10 == 1 && mod100 != 11) {
	        return i + "st";
	    } else if(mod10 == 2 && mod100 != 12) {
	        return i + "nd";
	    } else if(mod10 == 3 && mod100 != 13) {
	        return i + "rd";
	    } else {
	        return i + "th";
	    }
	}
	
	public static void main(String[] args) {
		
		Scanner in = new Scanner(System.in);
		
		System.out.println("FTCScores Real-time OPR library\r\n");
		System.out.print("Enter event link: https://ftcscores.com/event/");
		String link = in.nextLine();
		
		System.out.println("Getting event '" + link + "'...");
		REST.Response r = REST.request("https://api.ftcscores.com/api/events/" + link, Method.GET);
		
		if (r.bad()) {
			System.err.println(r.raw());
			System.err.println("Bad status: " + r.status());
			System.exit(1);
		}
		
		//Get result
		Requests.Event event = (Event) r.json(Requests.Event.class);
		
		//Store into matrix
		//Based on http://www.chiefdelphi.com/media/papers/download/3321
		
		int N = event.rankings.length;		//assumption: rankings are SORTED
		double[][] M = new double[N][N]; 	//matches each team played with other teams, organized by team rank
		double[] B_OPR = new double[N];		//score of all of that team's matches - highest rank first
		double[] B_CCWM = new double[N];	//winning margins of each match - highest rank first
		
		System.out.println("Preparing matrix for computation...");
		
		//for each team, get all matches that team participated in
		//TODO yeah, yeah, O(n^2). I know.
		for (int i=0; i<N; i++) {
			int team = event.rankings[i].number;
			ArrayList<Requests.Event.Match> matches = new ArrayList<>(10);
			int sumScore = 0;
			int diffScore = 0;
			//search for all matches played by that team
			for (int j=0; j<event.matches.length; j++) {
				//search all teams in a given match
				//constant time here - only 4 or 6 teams/match
				for (int k=0; k<event.matches[j].teams.red.length; k++) {
					if (event.matches[j].teams.red[k].number == team)
					{
						matches.add(event.matches[j]);
						sumScore += event.matches[j].scores.red;
						diffScore += event.matches[j].scores.red - event.matches[j].scores.blue;
						
						//append team participation to matrix
						//for each match (only counts those on same alliance)
						for (int l=0; l<event.matches[j].teams.red.length; l++) {
							//add participation count to that column
							M[i][event.matches[j].teams.red[l].rank-1]++;
						}
						
						break;
					}	
				}
				for (int k=0; k<event.matches[j].teams.blue.length; k++) {
					if (event.matches[j].teams.blue[k].number == team)
					{
						matches.add(event.matches[j]);
						sumScore += event.matches[j].scores.blue;
						diffScore += event.matches[j].scores.blue - event.matches[j].scores.red;
						
						//append team participation to matrix
						//for each match (only counts those on same alliance)
						for (int l=0; l<event.matches[j].teams.blue.length; l++) {
							//add participation count to that column
							M[i][event.matches[j].teams.blue[l].rank-1]++;
						}
						break;
					}
				}
			}
			
			//set sum of scores to right-hand side vector
			B_OPR[i] = sumScore;
			B_CCWM[i] = diffScore;
		}
		
		//Print matrix
//		for (int i=0; i<N; i++) {
//			for (int j=0; j<N; j++) {
//				System.out.printf("%2d ", (int)M[i][j]);
//			}
//			System.out.printf("  rank %2d = %f", i+1, B_CCWM[i]);
//			System.out.println();
//		}
		
		//Decompose system of equations (Cholesky)
		//Ax = B
		Jama.Matrix A = new Jama.Matrix(M);
		System.out.println("Decomposing...");
		Jama.CholeskyDecomposition decomp = A.chol();
		double[] opr = decomp.solve(new Jama.Matrix(B_OPR, 1).transpose()).transpose().getArray()[0];
		double[] ccwm = decomp.solve(new Jama.Matrix(B_CCWM, 1).transpose()).transpose().getArray()[0];
		
		//Sort and return OPRs, CCWMs
		TeamOPR[] teams = new TeamOPR[N];
		for (int i=0; i<N; i++) {
			teams[i] = new TeamOPR(event.rankings[i], opr[i], ccwm[i]);
		}
		Arrays.sort(teams, Comparator.comparing((TeamOPR team) -> team.getOPR()).reversed());
		
		//Print output
		for (int i=0; i<N; i++) {
			System.out.printf("%4s place: OPR: %7.3f CCWM: %7.3f  Team %5d %s",
					ordinate(teams[i].getTeamRank()), teams[i].getOPR(), teams[i].getCCWM(), teams[i].getTeamNumber(), teams[i].getTeamName());
			System.out.println();
		}
		
		in.close();
	}

}
