package com.arthurpachachura.oprlib;

import java.io.Serializable;

class Requests {
	
	@SuppressWarnings("serial")
	static class Event implements Serializable {
		String fullName, link, location, program, shortName, status, subtitle, type;
		Match[] matches;
		Ranking[] rankings;
		boolean isFinals;
		
		static class Match implements Serializable {
			String number, status;
			int order;
			Scores scores;
			Subscores subscoresRed, subscoresBlue;
			Teams teams;
			
			static class Scores implements Serializable {
				int red, blue;
			}
			static class Subscores implements Serializable {
				int auto, tele, endg, pen;
			}
			static class Teams implements Serializable {
				TeamMatch[] blue;
				TeamMatch[] red;
				
				static class TeamMatch implements Serializable {
					String name;
					int number, rank;
					boolean surrogate;
				}
			}
		}
		
		static class Ranking implements Serializable {
			String name;
			int number, rank;
		}
	}
}
