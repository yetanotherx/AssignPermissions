package yetanotherx.bukkitplugin.AssignPermissions;

//Java imports
import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

//Bukkit imports
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

//Permissions imports
import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

/*
 * AssignPermissions Version 1.0 - Assign groups from the game
 * Copyright (C) 2011 Yetanotherx <yetanotherx -a--t- gmail -dot- com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class AssignPermissions extends JavaPlugin {

    /**
     * Logger magic
     */
    public static final Logger log = Logger.getLogger("Minecraft");
    public static final Logger perm_log = Logger.getLogger("yetanotherx.bukkitplugin.AssignPermissions.Logger");

    /**
     * Permission plugin
     */
    public static PermissionHandler Permissions = null;

    
    /**
     * Outputs a message when disabled
     */
    public void onDisable() {
	log.info( "[" + this.getDescription().getName() + "]" + " Plugin disabled. (version" + this.getDescription().getVersion() + ")");
    }
    
    /**
     * Setup Permissions plugin & config files
     */
    public void onEnable() {

	setupPermissions();

	try {
	    setupLogging();
	} catch (SecurityException e) {
	    e.printStackTrace();
	    onDisable();
	} catch (IOException e) {
	    e.printStackTrace();
	    onDisable();
	}

	//Print that the plugin has been enabled!
	log.info( "[" + this.getDescription().getName() + "]" + " Plugin enabled! (version" + this.getDescription().getVersion() + ")");		
    }
    
    

    /**
     * Checks that Permissions is installed.
     */
    public void setupPermissions() {

	Plugin perm_plugin = this.getServer().getPluginManager().getPlugin("Permissions");
	PluginDescriptionFile pdfFile = this.getDescription();

	if( Permissions == null ) {
	    if( perm_plugin != null ) {
		//Permissions found, enable it now
		this.getServer().getPluginManager().enablePlugin( perm_plugin );
		Permissions = ( (Permissions) perm_plugin ).getHandler();
	    }
	    else {
		//Permissions not found. Disable plugin
		log.info(pdfFile.getName() + " version " + pdfFile.getVersion() + "not enabled. Permissions not detected");
		this.getServer().getPluginManager().disablePlugin(this);
	    }
	}
    }

    /**
     * Setup the local logger class, to keep track of who is setting permissions
     * @throws SecurityException
     * @throws IOException
     */
    private void setupLogging() throws SecurityException, IOException {

	new File("plugins" + File.separator + "AssignPermissions" + File.separator).mkdirs();

	Handler handler = new FileHandler("plugins" + File.separator + "AssignPermissions" + File.separator + "perm_log.log");
	handler.setFormatter(new SimpleFormatter());
	perm_log.addHandler(handler);

    }

    /**
     * Called when a user performs a command
     */
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

	String[] split = args;
	String commandName = command.getName().toLowerCase();

	if (sender instanceof Player) {
	    Player player = (Player) sender;

	    if (commandName.equals("perm-add")) {

		if (split.length == 2) {

		    if( Permissions.has(player, "assignpermissions.add.all") || Permissions.has(player, "assignpermissions.add.group." + split[1] ) ) {
			Configuration config = this.getConfig( player );

			if( inGroup( split[0], split[1], config ) ) {
			    player.sendMessage(ChatColor.RED + split[0] + " is already in the " + ChatColor.WHITE + split[1] + ChatColor.RED + " group!");
			    return true;
			}
			else {
			    if( config.getProperty( "groups." + split[1] ) != null ) {
				perm_log.info( "[AssignPermissions] " + player.getName() + " - Adding " + split[0] + " to " + split[1] + " user group.");
				config.setProperty( "users." + split[0] + ".group", split[1] );
				config.save();
				player.sendMessage(ChatColor.AQUA + split[0] + " is now in the " + ChatColor.WHITE + split[1] + ChatColor.AQUA + " group!");
				return true;
			    }
			    else {
				player.sendMessage(ChatColor.WHITE + split[1] + ChatColor.RED + " is not a valid group!");
				return true;
			    }
			}

		    }
		    else {
			return true;
		    }

		}
		else {
		    return false;
		}
	    }
	    if (commandName.equals("perm-del")) {

		if (split.length == 1) {

		    Configuration config = this.getConfig( player );

		    if( config.getString( "users." + split[0] + ".group" ) == null ) {
			player.sendMessage(ChatColor.RED + split[0] + " is not in a group!");
			return true;
		    }
		    else {

			if( Permissions.has(player, "assignpermissions.del.all") || Permissions.has(player, "assignpermissions.del.group." + config.getString( "users." + split[0] + ".group" ) ) ) {

			    config.setProperty( "users." + split[0] + ".group", "" );
			    config.save();
			    player.sendMessage(ChatColor.AQUA + split[0] + " is no longer in the " + ChatColor.WHITE + config.getString( "users." + split[0] + ".group" ) + ChatColor.AQUA + " group!");
			    return true;

			}
			else {
			    return true;
			}

		    }
		}
		else {
		    return false;
		}

	    }
	    if (commandName.equals("perm-list")) {

		if (split.length == 1) {

		    if( Permissions.has(player, "assignpermissions.list.all") || Permissions.has(player, "assignpermissions.list.group." + split[0] ) ) {
			Configuration config = this.getConfig( player );

			if( config.getProperty( "groups." + split[0] ) != null ) {

			    String out = "";

			    for( String user : config.getKeys("users") ) {
				if( config.getString( "users." + user + ".group" ).equals(split[0]) ) {
				    out = out + user + ", ";
				}
			    }
			    out = out.substring(0, out.length() - 2 );

			    player.sendMessage(ChatColor.AQUA + "Users in the " + split[0] + " group: " + ChatColor.WHITE + out );
			    return true;
			}
			else {
			    player.sendMessage(ChatColor.WHITE + split[0] + ChatColor.RED + " is not a valid group!");
			    return true;
			}

		    }
		    else {
			return true;
		    }

		}
		else {
		    return false;
		}
	    }
	}
	return false;
    }

    private Configuration getConfig( Player player ) {


	File file = new File("plugins" + File.separator + "Permissions", player.getWorld().getName() + ".yml");

	if( file.exists() ) {
	    Configuration config  = new Configuration(file);
	    config.load();
	    return config;
	}
	return null;

    }

    private boolean inGroup( String player, String group, Configuration config ) {
	if( config.getProperty( "users." + player ) != null ) {

	    if( config.getString( "users." + player + ".group" ).equalsIgnoreCase(group) ) {
		return true;
	    }
	    return false;
	}
	else {
	    return false;
	}

    }

}
