/**
 * Transport-level update ingestion contracts.
 *
 * <p>Package layout:
 * <ul>
 *   <li>source side: polling/webhook transport boundaries</li>
 *   <li>pipeline side: unified update delivery to consumer</li>
 *   <li>result side: typed statuses for transport adapters</li>
 * </ul>
 *
 * <p>Naming conventions:
 * <ul>
 *   <li>{@code *Source} for update producers</li>
 *   <li>{@code *Receiver} for HTTP webhook ingress adapters</li>
 *   <li>{@code *Pipeline} for shared ingestion flow</li>
 *   <li>{@code *Result}/{@code *Status} for transport outcomes</li>
 * </ul>
 */
package ru.tardyon.botframework.ingestion;
