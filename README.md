
# SimpleRepair - A simple and lightweight repair plugin with economy support

## 🔮 Features

- Repair holding item with `/repair` or `/fix` `(simplerepair.repair)`
- Check repairing cost with `/repairc` or `/fixc` `(simplerepair.repair)`
- Reload configuration `/simplerepair reload` `(simplerepair.reload)`
- All messages are fully customizable
- Cost are calculated based on the amount of damage the tool was given.

## 📄 Example Config

```yaml
# SimpleRepair Configuration File

# Base cost for repairing an item
base-cost: 10.0

# Additional cost per percentage of damage
cost-percentage: 10.0

# Messages
messages:
  prefix: "&#cac2ff&l🧰&r &7&l>>&r "
  no-console: "&cThis command can only be executed by a player."
  must-hold-item: "&cYou must hold an item in your hand to use this command."
  not-repairable: "&cThis item cannot be repaired."
  not-damaged: "&cThe item is not damaged and doesn't need repairing."
  not-enough-money: "&cYou don't have enough money to repair this item."
  item-repaired: "&aItem repaired for &e$%cost%&a."
  repair-cost: "&aRepair cost: &e$%cost%."
  simplerepair-info: "&fSimpleRepair by &#d1ffcfBelikhun"
  simplerepair-command-list: 
    - "&e/repair - Repair the item in your hand"
    - "&e/repairc - Calculate the repair cost"
    - "&e/simplerepair reload - Reload the configuration"
  simplerepair-reload-success: "&aConfiguration reloaded!"
  no-permission: "&cYou don't have permission to use this command."
```
